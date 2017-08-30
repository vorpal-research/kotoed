package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.DENIZEN
import org.jetbrains.research.kotoed.database.Tables.SUBMISSION_COMMENT
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.plus

private typealias CommentsResponse = SubmissionComments.CommentsResponse
private typealias CommentAggregate = SubmissionComments.CommentAggregate
private typealias CommentAggregatesByFile = MutableMap<String, SubmissionComments.CommentAggregate>
private typealias CommentAggregates = SubmissionComments.CommentAggregates

@AutoDeployable
class SubmissionVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Submission.Create)
    suspend fun handleCreate(submission: SubmissionRecord): DbRecordWrapper {
        val eb = vertx.eventBus()
        submission.id = null
        submission.datetime = null
        submission.state = SubmissionState.pending
        expect(submission.projectId is Int)

        val res: SubmissionRecord = dbCreateAsync(submission)
        dbProcessAsync(res)
        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordWrapper {
        val res: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(res)
        return DbRecordWrapper(res, status)
    }

    private suspend fun findSuccessorAsync(submission: SubmissionRecord): SubmissionRecord {
        var fst = dbFindAsync(SubmissionRecord().apply { parentSubmissionId = submission.id }).firstOrNull()
        while (fst != null) {
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
        return DbRecordWrapper(last, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Error)
    suspend fun handleError(verificationData: VerificationData): List<SubmissionStatusRecord> =
            verificationData.errors
                    .map { dbFetchAsync(SubmissionStatusRecord().apply { id = it }) }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Verification.Data)
    suspend fun handleVerificationData(submission: SubmissionRecord): VerificationData =
            dbVerifyAsync(submission)

    @JsonableEventBusConsumerFor(Address.Api.Submission.Verification.Clean)
    suspend fun handleVerificationClean(submission: SubmissionRecord): VerificationData =
            dbCleanAsync(submission)

    private fun CommentAggregatesByFile.register(key: String, comment: SubmissionCommentRecord) {
        this[key] = this.getOrDefault(key, CommentAggregate()).apply {
            register(comment)
        }
    }

    private fun SubmissionCommentRecord.isLost(): Boolean = sourcefile == SubmissionComments.UnknownFile ||
            sourceline == SubmissionComments.UnknownLine

    private fun List<SubmissionCommentRecord>.aggregateByFile(): CommentAggregates {
        val byFile = mutableMapOf<String, CommentAggregate>()

        val lost = CommentAggregate()

        for (comment in this) {
            if (comment.isLost())
                lost.register(comment)
            else {
                val path = comment.sourcefile.split('/')
                var current = ""
                for (chunk in path) {
                    current += chunk
                    byFile.register(current, comment)
                    current += "/"
                }
            }

        }

        return CommentAggregates(byFile = byFile, lost = lost)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comments)
    suspend fun handleComments(message: SubmissionRecord): CommentsResponse {
        val comments = dbQueryAsync(
                ComplexDatabaseQuery(table = SUBMISSION_COMMENT.name)
                        .find(SubmissionCommentRecord().apply { submissionId = message.id })
                        .join(DENIZEN.name, SUBMISSION_COMMENT.AUTHOR_ID.name)
                        .join(SUBMISSION_COMMENT.name, SUBMISSION_COMMENT.PERSISTENT_COMMENT_ID.name,
                                "original", SUBMISSION_COMMENT.PERSISTENT_COMMENT_ID.name)
                        .filter("original.${SUBMISSION_COMMENT.ORIGINAL_SUBMISSION_ID.name} == " +
                                "original.${SUBMISSION_COMMENT.SUBMISSION_ID.name}")
        ).map {
            Tuple() +
                    it.toRecord<SubmissionCommentRecord>() +
                    it.getJsonObject("author").toRecord<DenizenRecord>() +
                    it.getJsonObject("original").toRecord<SubmissionCommentRecord>()
        }

        val byFile = comments.filterNot { (it, _, _) ->
            it.isLost()
        }.groupBy { (it, _, _) ->
            it.sourcefile
        }.mapValues { (_, fileComments) ->
            fileComments.groupBy { (it, _, _) ->
                it.sourceline
            }.map { (line, lineComments) ->
                SubmissionComments.LineComments(line, lineComments.sortedBy { it.v0.datetime }.map { (comment, author, original) ->
                    comment.toJson().apply {
                        put("denizen_id", author.denizenId)
                        put("original", original.toJson())
                    }
                })
            }
        }.map { (file, fileComments) ->
            SubmissionComments.FileComments(file, fileComments)
        }

        val lost = comments.filter {
            it.v0.isLost()
        }

        return CommentsResponse(byFile = byFile, lost = lost.map { (comment, author, original) ->
            comment.toJson().apply {
                put("denizen_id", author.denizenId)
                put("original", original.toJson())
            }
        })
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.CommentAggregates)
    suspend fun handleCommentAggregates(message: SubmissionRecord): CommentAggregates =
            dbFindAsync(SubmissionCommentRecord().apply { submissionId = message.id }).aggregateByFile()


    @JsonableEventBusConsumerFor(Address.Api.Submission.List)
    suspend fun handleList(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE

        val currentPage = query.currentPage ?: 0
        val q = ComplexDatabaseQuery(Tables.SUBMISSION.name)
                .find(SubmissionRecord().apply {
                    projectId = query.find?.getInteger(Tables.SUBMISSION.PROJECT_ID.name)
                })
                .filter("state != \"${SubmissionState.obsolete}\"")
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val resp: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query(Tables.SUBMISSION.name), q)

        val reqWithVerificationData = if (query.withVerificationData ?: false) {
            resp.map { json ->
                val record: SubmissionRecord = json.toRecord()
                val vd = dbProcessAsync(record)
                json["verificationData"] = vd.toJson()
                json
            }
        } else resp

        return JsonArray(reqWithVerificationData)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.ListCount)
    suspend fun handleListCount(query: SearchQuery): JsonObject {
        val q = ComplexDatabaseQuery(Tables.SUBMISSION.name)
                .find(SubmissionRecord().apply {
                    projectId = query.find?.getInteger(Tables.SUBMISSION.PROJECT_ID.name)
                })
                .filter("state != ${SubmissionState.obsolete}")
        return sendJsonableAsync(Address.DB.count(Tables.SUBMISSION.name), q)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.History)
    suspend fun handleHistory(query: Submission.SubmissionHistoryQuery): JsonArray {
        val limit = query.limit ?: Int.MAX_VALUE
        val history = mutableListOf<JsonObject>()
        var currentId = query.submissionId

        for (i in 0 until limit) {
            val oldest = dbFetchAsync(SubmissionRecord().apply { id = currentId }) ?:
                    throw NotFound("Submission ${query.submissionId} not found")
            history.add(oldest.toJson())
            currentId = oldest.parentSubmissionId ?: break
        }

        return JsonArray(history)
    }
}