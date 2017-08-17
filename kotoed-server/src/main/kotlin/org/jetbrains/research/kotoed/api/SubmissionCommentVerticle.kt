package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionCommentState
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.notification.NotificationType
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
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

        val ret = DbRecordWrapper(res, VerificationData.Processed)

        // notifications
        val thread: List<SubmissionCommentRecord> = dbFindAsync(SubmissionCommentRecord().apply {
            submissionId = comment.submissionId
            sourcefile = comment.sourcefile
            sourceline = comment.sourceline
        })

        thread.groupBy { it.authorId }
                .filterKeys { it != comment.authorId }
                .forEach { (author, _) ->
                    dbCreateAsync (
                            NotificationRecord().apply {
                                denizenId = author
                                type = NotificationType.COMMENT_REPLIED_TO.toString()
                                body = ret.record
                            }
                    )
                }

        return ret
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

        val res = DbRecordWrapper(dbUpdateAsync(comment), VerificationData.Processed)

        return res
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Search)
    suspend fun handleSearch(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE
        val currentPage = query.currentPage ?: 0
        val q = ComplexDatabaseQuery("submission_comment_text_search")
                .join(table = "denizen", field = "author_id")
                .filter("""document matches "${query.text}"""")
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val req: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query("submission_comment_text_search"), q)

        return req.map {
            it.toRecord<SubmissionCommentRecord>().toJson().apply {
                put("denizen_id", it["author", "denizen_id"])
            }
        }.let(::JsonArray)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): JsonObject {
        val q = ComplexDatabaseQuery("submission_comment_text_search")
                .join(table = "denizen", field = "author_id")
                .filter("""document matches "${query.text}"""")

        return sendJsonableAsync(Address.DB.count("submission_comment_text_search"), q)
    }
}
