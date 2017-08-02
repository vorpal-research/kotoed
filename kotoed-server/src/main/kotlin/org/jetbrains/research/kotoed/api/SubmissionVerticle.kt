package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.SubmissionComments
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson

private typealias CommentAggregate = SubmissionComments.CommentAggregate
private typealias CommentAggregatesByFile = MutableMap<String, SubmissionComments.CommentAggregate>
private typealias CommentAggregates = SubmissionComments.CommentAggregates

@AutoDeployable
class SubmissionVerticle : AbstractKotoedVerticle(), Loggable {

    // FIXME: insert teamcity calls to build the submission
    @JsonableEventBusConsumerFor(Address.Api.Submission.Create)
    suspend fun handleCreate(submission: SubmissionRecord): DbRecordWrapper {
        val eb = vertx.eventBus()
        submission.id = null
        submission.datetime = null
        submission.state = SubmissionState.pending
        expect(submission.projectId is Int)

        val res: SubmissionRecord = dbCreateAsync(submission)
        eb.send(Address.DB.process(submission.table.name), res.toJson())
        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordWrapper {
        var res: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(res)
        if(status.status == VerificationStatus.Processed
                && res.state != SubmissionState.open) {
            res = dbUpdateAsync(res.apply { state = SubmissionState.open })
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
        return DbRecordWrapper(last, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Error)
    suspend fun handleError(verificationData: VerificationData): List<SubmissionStatusRecord> =
            verificationData.errors
                    .map { dbFetchAsync(SubmissionStatusRecord().apply { id = it }) }

    private fun CommentAggregatesByFile.register(key: String, comment: SubmissionCommentRecord) {
        this[key] = this.getOrDefault(key, CommentAggregate()).apply {
            register(comment)
        }
    }


    private fun List<SubmissionCommentRecord>.aggregateByFile(): CommentAggregates {
        val byFile = mutableMapOf<String, CommentAggregate>()

        val lost = CommentAggregate()

        for (comment in this) {
            if (comment.sourcefile == null)
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
    suspend fun handleComments(message: SubmissionRecord): List<SubmissionComments.FileComments> =
            dbFindAsync(SubmissionCommentRecord().apply { submissionId = message.id }).groupBy {
                it.sourcefile
            }.mapValues { (_, fileComments) ->
                fileComments.groupBy {
                    it.sourceline
                }.map { (line, lineComments) ->
                    SubmissionComments.LineComments(line, lineComments.sortedBy { it.datetime }.map {
                        val denizen = dbFetchAsync(DenizenRecord().apply { id = it.authorId })
                        it.toJson().apply {
                            this["denizenId"] = denizen.denizenId
                        }
                    })
                }
            }.map { (file, fileComments) ->
                SubmissionComments.FileComments(file, fileComments)
            }

    @JsonableEventBusConsumerFor(Address.Api.Submission.CommentAggregates)
    suspend fun handleCommentAggregates(message: SubmissionRecord): CommentAggregates =
            dbFindAsync(SubmissionCommentRecord().apply { submissionId = message.id }).aggregateByFile()
}