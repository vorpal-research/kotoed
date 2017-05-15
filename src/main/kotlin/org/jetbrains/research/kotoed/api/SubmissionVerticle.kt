package org.jetbrains.research.kotoed.api

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class SubmissionVerticle : AbstractKotoedVerticle(), Loggable {

    // FIXME: insert teamcity calls to build the submission
    @JsonableEventBusConsumerFor(Address.Api.Submission.Create)
    suspend fun handleCreate(project: SubmissionRecord): DbRecordWrapper {
        val eb = vertx.eventBus()
        val res: SubmissionRecord = dbCreateAsync(project)
        eb.send(Address.DB.process(project.table.name), res.toJson())
        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordWrapper {
        val res: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(res)
        if(status.status == VerificationStatus.Processed) {
            dbUpdateAsync(res.apply { state = Submissionstate.open })
        }
        return DbRecordWrapper(res, status)
    }

    private suspend fun findSuccessorAsync(submission: SubmissionRecord): SubmissionRecord {
        var fst = dbFindAsync(SubmissionRecord().apply { parentSubmissionId = submission.id }).firstOrNull()
        while(fst != null) {
            val sub = dbFindAsync(SubmissionRecord().apply { parentSubmissionId = fst!!.id }).firstOrNull()
            sub ?: return fst
            fst = sub
        }
        return submission
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Last)
    suspend fun handleLast(submission: SubmissionRecord): DbRecordWrapper {
        val res: SubmissionRecord = dbFetchAsync(submission)
        val last = findSuccessorAsync(res)
        val status: VerificationData = dbProcessAsync(last)
        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Error)
    suspend fun handleError(verificationData: VerificationData): List<SubmissionStatusRecord> =
            verificationData.errors
                    .map { dbFetchAsync(SubmissionStatusRecord().apply { id = it }) }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Create)
    suspend fun handleCommentCreate(message: SubmissionCommentRecord): SubmissionCommentRecord = run {
        message.apply { originalSubmissionId = originalSubmissionId ?: submissionId }

        val submission = fetchByIdAsync(Tables.SUBMISSION, expectNotNull(message.submissionId))
        when (submission.state) {
            Submissionstate.open -> dbCreateAsync(message)
            Submissionstate.obsolete -> {
                log.warn("Comment request for an obsolete submission received:" +
                        "Submission id = ${submission.id}")
                val successor = expectNotNull(findSuccessorAsync(submission))
                log.trace("Newer submission found: id = ${successor.id}")
                message.submissionId = successor.id
                sendJsonableAsync(Address.Api.Submission.Comment.Create, message)
            }
            else -> throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Read)
    suspend fun handleCommentRead(message: SubmissionCommentRecord): SubmissionCommentRecord =
            dbFetchAsync(message)

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comments)
    suspend fun handleComments(message: SubmissionRecord): List<SubmissionCommentRecord> =
            dbFindAsync(SubmissionCommentRecord().apply { submissionId = message.id })

}