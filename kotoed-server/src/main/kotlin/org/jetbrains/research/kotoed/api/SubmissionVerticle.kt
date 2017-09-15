package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.notification.NotificationType
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.DENIZEN
import org.jetbrains.research.kotoed.database.Tables.SUBMISSION_COMMENT
import org.jetbrains.research.kotoed.database.enums.SubmissionCommentState
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
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

    private suspend fun notifyCreated(record: SubmissionRecord) {
        val parentSubmissionId = record.parentSubmissionId ?: return
        val jumboSub =
                dbQueryAsync(ComplexDatabaseQuery(Tables.SUBMISSION).find(SubmissionRecord().apply {
                    id = record.id
                }).join(ComplexDatabaseQuery(Tables.PROJECT).join(Tables.DENIZEN))).firstOrNull()

        val targets = dbFindAsync(SubmissionCommentRecord().apply {
            submissionId = parentSubmissionId
            state = SubmissionCommentState.open
        }).map{ it.authorId }.toSet() - jumboSub.safeNav("project", "denizen", "id") as? Int

        targets.forEach {
            createNotification(NotificationRecord().apply {
                denizenId = it
                type = NotificationType.RESUBMISSION.toString()
                body = JsonObject().apply {
                    this["author"] = jumboSub.safeNav("project", "denizen")
                    this["submissionId"] = record.id
                    this["oldSubmissionId"] = parentSubmissionId
                }
            })
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Create)
    suspend fun handleCreate(submission: SubmissionRecord): DbRecordWrapper {
        val eb = vertx.eventBus()
        submission.id = null
        submission.datetime = null
        submission.state = SubmissionState.pending
        expect(submission.projectId is Int)

        val res: SubmissionRecord = dbCreateAsync(submission)
        dbProcessAsync(res)
        val ret = DbRecordWrapper(res)

        launch(LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
            notifyCreated(res)
        }

        return ret
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordWrapper {
        val res: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(res)
        return DbRecordWrapper(res, status)
    }

    private suspend fun notifyUpdated(record: SubmissionRecord) {
        val parentSubmissionId = record.parentSubmissionId ?: return
        val jumboSub =
                dbQueryAsync(ComplexDatabaseQuery(Tables.SUBMISSION).find(SubmissionRecord().apply {
                    id = record.id
                }).join(ComplexDatabaseQuery(Tables.PROJECT).join(Tables.DENIZEN))).firstOrNull()

        createNotification(NotificationRecord().apply {
            denizenId = jumboSub.safeNav("project", "denizen", "id") as? Int
            type = NotificationType.SUBMISSION_UPDATE.toString()
            body = jumboSub
        }) // TODO by whom?
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Update)
    suspend fun handleUpdate(submission: SubmissionRecord): DbRecordWrapper {
        val existing = fetchByIdAsync(Tables.SUBMISSION, submission.id)

        val shouldBeDone = (existing.state == SubmissionState.open || existing.state == SubmissionState.closed) &&
                submission.state == SubmissionState.open || submission.state == SubmissionState.closed

        submission.apply {
            datetime = existing.datetime
            parentSubmissionId = existing.parentSubmissionId
            projectId = existing.projectId
            revision = existing.revision
            state = if (!shouldBeDone)
                existing.state
            else
                state

        }

        val updated = dbUpdateAsync(submission)

        val vd = dbProcessAsync(updated)

        val ret = DbRecordWrapper(updated, vd)

        if (shouldBeDone)
            launch(LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
                notifyUpdated(updated)
            }
        return ret
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

    @JsonableEventBusConsumerFor(Address.Api.Submission.CommentsTotal)
    suspend fun handleCommentsTotal(message: SubmissionRecord): CommentAggregate =
            CommentAggregate(
                    mutableMapOf(
                            SubmissionCommentState.open to
                                    dbCountAsync(
                                            ComplexDatabaseQuery(Tables.SUBMISSION_COMMENT)
                                                    .find(SubmissionCommentRecord().apply {
                                                        submissionId = message.id
                                                        state = SubmissionCommentState.open
                                                    })).getInteger("count"),
                            SubmissionCommentState.closed to
                                    dbCountAsync(
                                            ComplexDatabaseQuery(Tables.SUBMISSION_COMMENT)
                                                    .find(SubmissionCommentRecord().apply {
                                                        submissionId = message.id
                                                        state = SubmissionCommentState.closed
                                                    })).getInteger("count")

                    )

            )

    @JsonableEventBusConsumerFor(Address.Api.Submission.List)
    suspend fun handleList(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE

        val currentPage = query.currentPage ?: 0
        val q = ComplexDatabaseQuery(Tables.SUBMISSION.name)
                .find(SubmissionRecord().apply {
                    projectId = query.find?.getInteger(Tables.SUBMISSION.PROJECT_ID.name)
                })
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val stateIn = query.find?.getJsonArray("state_in")?.toList()?.mapNotNull { it as? String }

        val qFiltered = stateIn?.let { q.filter("state in %s".formatToQuery(it)) } ?: q

        val resp: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query(Tables.SUBMISSION.name), qFiltered)

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

        val stateIn = query.find?.getJsonArray("state_in")?.toList()?.mapNotNull { it as? String }

        val qFiltered = stateIn?.let { q.filter("state in %s".formatToQuery(it)) } ?: q

        return sendJsonableAsync(Address.DB.count(Tables.SUBMISSION.name), qFiltered)
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

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.Create)
    suspend fun handleTagsCreate(submissionTag: SubmissionTagRecord): SubmissionTagRecord {
        return dbCreateAsync(submissionTag)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.Read)
    suspend fun handleTagsRead(submission: SubmissionRecord): List<TagRecord> {
        val query = ComplexDatabaseQuery(Tables.SUBMISSION_TAG)
                .join(Tables.TAG)
                .find(SubmissionTagRecord().apply { submissionId = submission.id })

        val res = dbQueryAsync(query)

        return res.map { it.getJsonObject("tag") }
                .filterNotNull()
                .map { it.toRecord<TagRecord>() }
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.Update)
    suspend fun handleTagsUpdate(query: Submission.TagUpdateQuery): JsonObject {
        return sendJsonableAsync(
                Address.DB.Submission.Tags.Update,
                query
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.Delete)
    suspend fun handleTagsDelete(submissionTag: SubmissionTagRecord): List<SubmissionTagRecord> {
        val dbSubmissionTags = dbFindAsync(submissionTag)
        dbSubmissionTags.forEach { dbDeleteAsync(it) }
        return dbSubmissionTags
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.Search)
    suspend fun handleSearchForTag(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE
        val currentPage = query.currentPage ?: 0

        val denizenQ = ComplexDatabaseQuery("project")
                .join("denizen")

        val projectQ = ComplexDatabaseQuery("submission")
                .join(denizenQ)

        val q = ComplexDatabaseQuery("submission_tag")
                .join(projectQ)
                .join("tag")
                .filter("tag.name == %s".formatToQuery(query.text))
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val req: List<JsonObject> =
                sendJsonableCollectAsync(Address.DB.query("submission_tag"), q)

        return req.map { sub ->
            sub
        }.let(::JsonArray)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Tags.SearchCount)
    suspend fun handleSearchForTagCount(query: SearchQuery): JsonObject {
        val denizenQ = ComplexDatabaseQuery("project")
                .join("denizen")

        val projectQ = ComplexDatabaseQuery("submission")
                .join(denizenQ)

        val q = ComplexDatabaseQuery("submission_tag")
                .join(projectQ)
                .join("tag")
                .filter("tag.name == %s".formatToQuery(query.text))

        return sendJsonableAsync(Address.DB.count("submission_tag"), q)
    }
}
