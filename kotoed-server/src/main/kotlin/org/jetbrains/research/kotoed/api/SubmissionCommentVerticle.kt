package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.enums.Submissioncommentstate
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class SubmissionCommentVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Create)
    suspend fun handleCreate(comment: SubmissionCommentRecord): DbRecordWrapper {
        comment.id = null
        comment.originalSubmissionId = comment.submissionId // NOTE: this is unconditional for a reason
        comment.state = Submissioncommentstate.open

        val submission: SubmissionRecord = fetchByIdAsync(SubmissionRecord().table, comment.submissionId)
        val res = when (submission.state) {
            Submissionstate.open -> dbCreateAsync(comment)
            Submissionstate.obsolete -> {
                log.warn("Comment request for an obsolete submission received: " +
                        "Submission id = ${submission.id}")
                val wrappedSuccessor: DbRecordWrapper =
                        sendJsonableAsync(
                                Address.Api.Submission.Last,
                                submission
                        )
                if(wrappedSuccessor.verificationData.status != VerificationStatus.Processed) {
                    throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
                }
                val successor: SubmissionRecord = wrappedSuccessor.record.toRecord()
                log.trace("Newer submission found: id = ${successor.id}")
                comment.submissionId = successor.id
                // recursive message
                return handleCreate(comment)
            }
            else -> throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
        }
        return DbRecordWrapper(res, VerificationData.Processed)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Read)
    suspend fun handleRead(comment: SubmissionCommentRecord) = DbRecordWrapper(dbFetchAsync(comment), VerificationData.Processed)


}
