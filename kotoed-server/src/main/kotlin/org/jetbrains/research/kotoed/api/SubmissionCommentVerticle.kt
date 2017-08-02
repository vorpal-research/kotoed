package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionCommentState
import org.jetbrains.research.kotoed.database.enums.SubmissionState
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
        comment.state = SubmissionCommentState.open

        val submission: SubmissionRecord = fetchByIdAsync(SubmissionRecord().table, comment.submissionId)
        val res = when (submission.state) {
            SubmissionState.open -> dbCreateAsync(comment)
            SubmissionState.obsolete -> {
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
    suspend fun handleRead(comment: SubmissionCommentRecord) =
            DbRecordWrapper(dbFetchAsync(comment), VerificationData.Processed)

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Update)
    suspend fun handleUpdate(comment: SubmissionCommentRecord): DbRecordWrapper {
        val existing = fetchByIdAsync(Tables.SUBMISSION_COMMENT, comment.id)
        existing.id ?: throw NotFound("Comment ${comment.id} not found")
        if(comment.text == null && existing.text != null) comment.text = existing.text
        if(comment.state == null) comment.state = existing.state
        comment.submissionId         = existing.submissionId
        comment.datetime             = existing.datetime
        comment.sourcefile           = existing.sourcefile
        comment.sourceline           = existing.sourceline
        comment.authorId             = existing.authorId
        comment.persistentCommentId  = existing.persistentCommentId
        return DbRecordWrapper(dbUpdateAsync(comment), VerificationData.Processed)
    }


}
