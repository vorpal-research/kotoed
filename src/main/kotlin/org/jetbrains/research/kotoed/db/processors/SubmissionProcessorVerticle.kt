package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.ForeignKey

@AutoDeployable
class SubmissionProcessorVerticle : ProcessorVerticle<SubmissionRecord>(Tables.SUBMISSION) {

    // parent submission id can be invalid, filter it out
    override val checkedReferences: List<ForeignKey<SubmissionRecord, *>>
        get() = super.checkedReferences.filterNot { Tables.SUBMISSION.PARENT_SUBMISSION_ID in it.fieldsArray }

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
                            "comment.id = ${comment.persistentCommentId}")
            { it.size == 1 }
                    .first()
        }

        val parentComments =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = parent.id })

        val alreadyMappedPersistentIds =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = child.id }).map { it.persistentCommentId }

        // first, we create all the missing comments
        var childComments: List<SubmissionCommentRecord> =
                parentComments
                        .asSequence()
                        .filter { it.persistentCommentId !in alreadyMappedPersistentIds }
                        .mapTo(mutableListOf()) { comment ->
                            dbCreateAsync(comment.copy().apply { submissionId = child.id })
                        }

        // second, we remap all the locations and reply-chains

        childComments = childComments.map { comment ->
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
                comment.previousCommentId = childComments.find { it.persistentCommentId == prevAncestor.persistentCommentId }?.id
            }

            dbUpdateAsync(comment)
        }

        parent.state = Submissionstate.obsolete

        dbUpdateAsync(parent)
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {
        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)
        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let { fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId) }

        val vcsReq: RepositoryInfo =
                sendJsonableAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
                )

        if (vcsReq.status != CloneStatus.done) return VerificationData.Unknown

        if(sub.revision == null) {
            val vcsInfo: InfoFormat = sendJsonableAsync(Address.Code.Info, InfoFormat(uid = vcsReq.uid))
            sub.revision = vcsInfo.revision
            dbUpdateAsync(sub)
        }

        parentSub?.let {
            recreateCommentsAsync(vcsReq.uid, parentSub, sub)
        }

        return verify(data)
    }

    suspend override fun verify(data: JsonObject?): VerificationData {
        data ?: throw IllegalArgumentException("Cannot verify submission $data")
        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)
        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let { fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId) }

        val vcsReq: RepositoryInfo =
                sendJsonableAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
                )

        val vcsStatus =
                when (vcsReq.status) {
                    CloneStatus.pending -> VerificationData.Unknown
                    CloneStatus.done -> VerificationData.Processed
                    CloneStatus.failed ->
                        dbCreateAsync(
                                SubmissionStatusRecord().apply {
                                    this.submissionId = sub.id
                                    this.data = JsonObject(
                                            "error" to "Fetching remote repository failed",
                                            "reason" to vcsReq.toJson()
                                    )
                                }
                        ).id.let { VerificationData.Invalid(it) }
                }

        if (vcsStatus != VerificationData.Processed) return vcsStatus

        try {
            forceType<ListResponse>(sendJsonableAsync(Address.Code.List, ListRequest(vcsReq.uid, sub.revision)))
        } catch (ex: ReplyException) {
            return dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "error" to "Fetching revision ${sub.revision} for repository ${project.repoUrl} failed",
                                "reason" to ex.message
                        )
                    }
            ).id.let { VerificationData.Invalid(it) }
        }

        parentSub?.let {
            val parentComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = parentSub.id })

            val ourComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = sub.id })
                    .asSequence()
                    .map { it.persistentCommentId }
                    .toSet()

            if (parentSub.state != Submissionstate.obsolete) {
                return VerificationData.Unknown
            }

            if (!parentComments.all { it.persistentCommentId in ourComments }) {
                return VerificationData.Unknown
            }
        }

        return VerificationData.Processed
    }

}


