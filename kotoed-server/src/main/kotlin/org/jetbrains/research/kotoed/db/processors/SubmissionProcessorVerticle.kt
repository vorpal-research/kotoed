package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.buildbot.util.Kotoed2Buildbot
import org.jetbrains.research.kotoed.buildbot.verticles.BuildRequestPollerVerticle
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.buildbot.build.TriggerBuild
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.*
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.ForeignKey

data class BuildTriggerResult(
        val result: String,
        val buildRequestId: Int
) : Jsonable

@AutoDeployable
class SubmissionProcessorVerticle : ProcessorVerticle<SubmissionRecord>(Tables.SUBMISSION) {

    // parent submission id can be invalid, filter it out
    override val checkedReferences: List<ForeignKey<SubmissionRecord, *>>
        get() = super.checkedReferences
                .filterNot { Tables.SUBMISSION.PARENT_SUBMISSION_ID in it.fieldsArray }

    private val SubmissionCommentRecord.location
        get() = Location(Filename(path = sourcefile), sourceline)

    private suspend fun recreateCommentsAsync(vcsUid: String, parent: SubmissionRecord, child: SubmissionRecord) {
        val submissionCacheAsync = AsyncCache { id: Int -> fetchByIdAsync(Tables.SUBMISSION, id) }
        val commentCacheAsync = AsyncCache { id: Int -> fetchByIdAsync(Tables.SUBMISSION_COMMENT, id) }
        val ancestorCommentCacheAsync = AsyncCache { comment: SubmissionCommentRecord ->
            dbFindAsync(SubmissionCommentRecord().apply {
                submissionId = comment.originalSubmissionId
                persistentCommentId = comment.persistentCommentId
            }).expecting(
                    message = "Duplicate or missing comment in chain detected: " +
                            "submission.id = ${comment.originalSubmissionId} " +
                            "comment.id = ${comment.persistentCommentId}"
            ) { 1 == it.size }
                    .first()
        }

        val parentComments =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = parent.id })

        val alreadyMappedPersistentIds =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = child.id }).map { it.persistentCommentId }

        // first, we create all the missing comments

        val childComments: List<SubmissionCommentRecord> =
                parentComments
                        .asSequence()
                        .filter { it.persistentCommentId !in alreadyMappedPersistentIds }
                        .mapTo(mutableListOf()) { comment ->
                            dbCreateAsync(comment.copy().apply { submissionId = child.id })
                        }

        // second, we remap all the locations and reply-chains

        childComments.forEach { comment ->
            val ancestorComment = ancestorCommentCacheAsync(comment)
            val ancestorSubmission = submissionCacheAsync(ancestorComment.submissionId)

            val adjustedLocation: LocationResponse =
                    sendJsonableAsync(
                            Address.Code.LocationDiff,
                            LocationRequest(
                                    vcsUid,
                                    ancestorComment.location,
                                    ancestorSubmission.revision,
                                    child.revision
                            )
                    )
            comment.sourcefile = adjustedLocation.location.filename.path
            comment.sourceline = adjustedLocation.location.line

            if (comment.previousCommentId != null) {
                val prevAncestor = ancestorCommentCacheAsync(commentCacheAsync(comment.previousCommentId))
                val previousComment = childComments.find { it.persistentCommentId == prevAncestor.persistentCommentId }
                comment.previousCommentId = previousComment?.id
            }

            dbUpdateAsync(comment)
        }

        data class DialoguePoint(var prev: DialoguePoint? = null, val value: SubmissionCommentRecord) {
            val head: DialoguePoint get() = prev?.head?.also { prev = it } ?: this
        }

        val dialogues = childComments.map { it.id to DialoguePoint(value =  it) }.toMap()
        dialogues.forEach { (_, v) ->
            v.prev = dialogues[v.value.previousCommentId]
        }
        dialogues.forEach { (_, v) ->
            v.value.sourceline = v.head.value.sourceline
            v.value.sourcefile = v.head.value.sourcefile
            dbUpdateAsync(v.value)
        }
    }

    private suspend fun copyTagsFrom(parent: SubmissionRecord, child: SubmissionRecord) {
        val parentTags = dbFindAsync(
                SubmissionTagRecord().apply { submissionId = parent.id })

        try {
            dbBatchCreateAsync(parentTags.map { it.apply { submissionId = child.id } })
        } catch (ex: Exception) {
            // FIXME: Do nothing?
        }
    }

    private suspend fun getVcsInfo(project: ProjectRecord): RepositoryInfo {
        return sendJsonableAsync(
                Address.Code.Download,
                RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
        )
    }

    private suspend fun getVcsStatus(
            vcsInfo: RepositoryInfo,
            submission: SubmissionRecord): VerificationData {

        return when (vcsInfo.status) {
            CloneStatus.pending -> VerificationData.Unknown
            CloneStatus.done -> VerificationData.Processed
            CloneStatus.failed ->
                dbCreateAsync(
                        SubmissionStatusRecord().apply {
                            this.submissionId = submission.id
                            this.data = JsonObject(
                                    "failure" to "Fetching remote repository failed",
                                    "details" to vcsInfo.toJson()
                            )
                        }
                ).id.let { VerificationData.Invalid(it) }
        }
    }

    suspend override fun doProcess(data: JsonObject): VerificationData = run {
        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)
        val owner: DenizenRecord = fetchByIdAsync(Tables.DENIZEN, project.denizenId)

        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let {
            fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId)
        }

        parentSub?.let {
            it.state = SubmissionState.obsolete
            dbUpdateAsync(it)
        }

        val vcsReq = getVcsInfo(project)

        val vcsStatus = getVcsStatus(vcsReq, sub)

        if (vcsStatus != VerificationData.Processed) return@run vcsStatus

        if (sub.revision == null) {
            val vcsInfo: InfoFormat = sendJsonableAsync(Address.Code.Info, InfoFormat(uid = vcsReq.uid))
            sub.revision = vcsInfo.revision
            dbUpdateAsync(sub)
        }

        parentSub?.let {
            recreateCommentsAsync(vcsReq.uid, it, sub)

            copyTagsFrom(it, sub)
        }

        val buildInfos = dbFindAsync(BuildRecord().apply { submissionId = sub.id })

        val localVerificationData = try {

            when (buildInfos.size) {
                0 -> {
                    val btr: BuildTriggerResult = sendJsonableAsync(
                            Address.Buildbot.Build.Trigger,
                            TriggerBuild(
                                    Kotoed2Buildbot.projectName2schedulerName(
                                            Kotoed2Buildbot.asBuildbotProjectName(
                                                    owner.denizenId, project.name)),
                                    sub.revision
                            )
                    )

                    dbCreateAsync(
                            BuildRecord().apply {
                                submissionId = sub.id
                                buildRequestId = btr.buildRequestId
                            }
                    )

                    VerificationData.Processed
                }
                1 -> {
                    val buildInfo = buildInfos[0]

                    val buildResults = dbFindAsync(
                            SubmissionResultRecord().apply { submissionId = sub.id })

                    if (buildResults.isEmpty()) {
                        log.info("Retrying polling for build request id: $buildInfo.buildRequestId")
                        vertx.deployVerticle(
                                BuildRequestPollerVerticle(buildInfo.buildRequestId))
                    }

                    VerificationData.Processed
                }
                else -> {
                    val errorId = dbCreateAsync(
                            SubmissionStatusRecord().apply {
                                this.submissionId = sub.id
                                this.data = JsonObject(
                                        "failure" to "Several builds found for submission ${sub.id}",
                                        "details" to buildInfos.tryToJson()
                                )
                            }
                    ).id

                    return VerificationData.Invalid(errorId)
                }
            }

        } catch (ex: Exception) {
            val errorId = dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "failure" to "Triggering build for ${project.name}:${sub.id} failed",
                                "details" to ex.message
                        )
                    }
            ).id

            VerificationData.Invalid(errorId)
        }

        return@run localVerificationData and verify(data)

    }.apply {
        val sub: SubmissionRecord = data.toRecord()
        val subFresh = dbFetchAsync(sub)
        if (status == VerificationStatus.Processed
                && subFresh.state == SubmissionState.pending) {
            dbUpdateAsync(subFresh.apply { state = SubmissionState.open })
        }
    }

    suspend override fun verify(data: JsonObject?): VerificationData {
        data ?: throw IllegalArgumentException("Cannot verify null submission")

        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)
        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let {
            fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId)
        }

        val vcsReq = getVcsInfo(project)

        val vcsStatus = getVcsStatus(vcsReq, sub)

        if (vcsStatus != VerificationData.Processed) return vcsStatus

        try {
            val list: ListResponse = sendJsonableAsync(
                    Address.Code.List,
                    ListRequest(vcsReq.uid, sub.revision)
            )

            list.ignore()

        } catch (ex: Exception) {
            val errorId = dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "failure" to "Fetching revision ${sub.revision} for repository ${project.repoUrl} failed",
                                "details" to ex.message
                        )
                    }
            ).id

            return VerificationData.Invalid(errorId)
        }

        parentSub?.let {
            val parentComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = parentSub.id })

            val ourComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = sub.id })
                    .asSequence()
                    .map { it.persistentCommentId }
                    .toSet()

            if (parentSub.state != SubmissionState.obsolete) {
                return VerificationData.Unknown
            }

            if (!parentComments.all { it.persistentCommentId in ourComments }) {
                return VerificationData.Unknown
            }
        }

        val buildInfos = dbFindAsync(BuildRecord().apply { submissionId = sub.id })

        return when (buildInfos.size) {
            1 -> VerificationData.Processed
            0 -> VerificationData.Unknown
            else -> {
                dbCreateAsync(
                        SubmissionStatusRecord().apply {
                            this.submissionId = sub.id
                            this.data = JsonObject(
                                    "failure" to "Several builds found for submission ${sub.id}",
                                    "details" to buildInfos.tryToJson()
                            )
                        }
                ).id.let { VerificationData.Invalid(it) }
            }
        }
    }

    suspend override fun doClean(data: JsonObject): VerificationData {
        val sub: SubmissionRecord = data.toRecord()

        async {
            val project = dbFetchAsync(ProjectRecord().apply { id = sub.projectId })
            sendJsonable(Address.Code.PurgeCache, RemoteRequest(vcs = null, url = project.repoUrl))
        }

        dbWithTransactionAsync {
            deleteFrom(SUBMISSION_STATUS)
                    .where(SUBMISSION_STATUS.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
            deleteFrom(SUBMISSION_RESULT)
                    .where(SUBMISSION_RESULT.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
            deleteFrom(BUILD)
                    .where(BUILD.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
        }

        return VerificationData.Unknown
    }

}
