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
        dbFindAsync(SubmissionCommentRecord().apply { submissionId = parent.id })
                .forEach { comment ->
                    comment.submissionId = child.id
                    val adjustedLocation: LocationResponse =
                            sendJsonableAsync(
                                    Address.Code.LocationDiff,
                                    LocationRequest(
                                            vcsUid,
                                            comment.location,
                                            parent.revision,
                                            child.revision
                                    )
                            )
                    comment.sourcefile = adjustedLocation.location.filename.path
                    comment.sourceline = adjustedLocation.location.line
                    dbCreateAsync(comment)
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

        if(vcsReq.status != CloneStatus.done) return VerificationData.Unknown

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

            if(parentSub.state != Submissionstate.obsolete) {
                return VerificationData.Unknown
            }

            if(!parentComments.all { it.persistentCommentId in ourComments }) {
                return VerificationData.Unknown
            }
        }

        return VerificationData.Processed
    }

}


