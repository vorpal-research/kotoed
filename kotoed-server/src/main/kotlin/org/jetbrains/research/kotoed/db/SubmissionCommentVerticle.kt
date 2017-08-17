package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.db.TextSearchRequest
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerForDynamic
import org.jetbrains.research.kotoed.util.database.*
import org.jetbrains.research.kotoed.util.expecting
import org.jooq.impl.DSL

@AutoDeployable
class SubmissionCommentVerticle : CrudDatabaseVerticleWithReferences<SubmissionCommentRecord>(Tables.SUBMISSION_COMMENT) {

    val fullAddress get() = Address.DB.full(entityName)
    val lastAddress get() = Address.DB.last(entityName)
    val textSearchAddress get() = Address.DB.searchText(entityName)

    suspend override fun handleCreate(message: SubmissionCommentRecord): SubmissionCommentRecord {
        log.trace("Create requested in table ${table.name}:\n" +
                message.toJson().encodePrettily())

        message.reset(Tables.SUBMISSION_COMMENT.ID)

        return db {
            with(Tables.SUBMISSION_COMMENT) {
                val previousCommentQuery =
                        select(ID)
                                .from(this@with)
                                .where(SUBMISSION_ID equal message.submissionId)
                                .and(SOURCEFILE equal message.sourcefile)
                                .and(SOURCELINE equal message.sourceline)
                                .orderBy(DATETIME.desc())
                                .limit(1)

                if(message[PREVIOUS_COMMENT_ID] != null) {
                    insertInto(table)
                            .set(message)
                            .returning()
                            .fetch()
                            .into(recordClass)
                            .first()
                } else {
                    insertInto(table)
                            .set(message)
                            .set(PREVIOUS_COMMENT_ID, previousCommentQuery)
                            .returning()
                            .fetch()
                            .into(recordClass)
                            .first()
                }
            }
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "fullAddress")
    suspend fun handleFull(query: SubmissionCommentRecord): JsonObject {
        val id = query.id

        val commentTable = Tables.SUBMISSION_COMMENT
        val authorTable = Tables.DENIZEN.`as`("author")
        val submissionTable = Tables.SUBMISSION.`as`("submission")
        val originalSubmissionTable = Tables.SUBMISSION.`as`("originalSubmission")

        val overRecords = db {
            select(*commentTable.fields(),
                    *authorTable.fields(),
                    *submissionTable.fields(),
                    *originalSubmissionTable.fields())
                    .from(commentTable)
                    .join(authorTable).onKey(commentTable.AUTHOR_ID)
                    .join(submissionTable).onKey(commentTable.SUBMISSION_ID)
                    .join(originalSubmissionTable).onKey(commentTable.ORIGINAL_SUBMISSION_ID)
                    .where(commentTable.ID equal id)
                    .fetch()
        }
        val overRecord = overRecords.expecting { it.size == 1 }.first()

        val comment = overRecord.into(commentTable)
        val author = overRecord.into(authorTable)
        val submission = overRecord.into(submissionTable)
        val originalSubmission = overRecord.into(originalSubmissionTable)

        val ret = comment.toJson().apply {
            remove("author_id")
            put("author", author.toJson())
            remove("submission_id")
            put("submission", submission.toJson())
            remove("original_submission_id")
            put("original_submission", originalSubmission.toJson())
        }
        return ret
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "lastAddress")
    suspend fun handleLast(query: SubmissionCommentRecord): SubmissionCommentRecord {
        val id = query.id

        val comment = Tables.SUBMISSION_COMMENT.`as`("comment")
        val otherComment = Tables.SUBMISSION_COMMENT.`as`("otherComment")
        val submission = Tables.SUBMISSION

        val overRecords = db {
            select(*otherComment.fields())
                    .from(comment, otherComment, submission)
                    .where(
                            (comment.ID equal id)
                                    and (comment.ORIGINAL_SUBMISSION_ID equal otherComment.ORIGINAL_SUBMISSION_ID)
                                    and (submission.STATE ne SubmissionState.obsolete)
                                    and (otherComment.SUBMISSION_ID equal submission.ID)
                    )
                    .fetchInto(otherComment)
        }
        return overRecords.expecting { it.size == 1 }.first()
    }


    @JsonableEventBusConsumerForDynamic(addressProperty = "textSearchAddress")
    suspend fun handleTextSearch(query: TextSearchRequest): List<SubmissionCommentRecord> {
        val searchTable = Tables.SUBMISSION_COMMENT_TEXT_SEARCH
        val queryText = query.text

        return db {
            selectFrom(searchTable)
                    .where(searchTable.DOCUMENT documentMatchPlain queryText)
                    .orderBy((searchTable.DOCUMENT documentMatchRankPlain queryText).desc())
                    .fetchInto(Tables.SUBMISSION_COMMENT)
        }
    }
}