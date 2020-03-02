package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.Submission
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.SubmissionTag
import org.jetbrains.research.kotoed.database.tables.records.SubmissionTagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.NotFound
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.into
import org.jooq.impl.DSL.unquotedName

@AutoDeployable
class SubmissionTagVerticle : CrudDatabaseVerticleWithReferences<SubmissionTagRecord>(Tables.SUBMISSION_TAG) {

    override suspend fun handleBatchCreate(message: List<SubmissionTagRecord>): List<SubmissionTagRecord> {
        table as SubmissionTag
        log.trace("Batch create requested in table ${table.name}:\n" +
                message.joinToString(separator = "\n")
        )

        if (message.isEmpty()) return emptyList()

        for (field in table.primaryKey.fieldsArray) {
            message.map { it.reset(field) }
        }

        return db {
            // we cannot ignore conflicts 'cos
            // ON CONFLICT DO NOTHING together with RETURNING
            // produces no result
            // so we update at least one of the fields
            sqlStateAware {
                withTransaction {
                    val excluded = table.`as`(unquotedName("excluded"))
                    message.drop(1)
                            .fold(insertInto(table)
                                    .set(message.first())) { acc, r ->
                                acc.newRecord().set(r)
                            }
                            .onConflict(table.TAG_ID, table.SUBMISSION_ID)
                            .doUpdate()
                            .set(table.TAG_ID, excluded.TAG_ID)
                            .returning()
                            .fetch()
                            .into(recordClass)
                }
            }
        }
    }

    override suspend fun handleCreate(message: SubmissionTagRecord): SubmissionTagRecord {
        log.trace("Create requested in table ${table.name}:\n$message")

        for (field in table.primaryKey.fieldsArray) {
            message.reset(field)
        }

        table as SubmissionTag

        return db {
            // we cannot ignore conflicts 'cos
            // ON CONFLICT DO NOTHING together with RETURNING
            // produces no result
            // so we update at least one of the fields
            sqlStateAware {
                val excluded = table.`as`(unquotedName("excluded"))
                insertInto(table)
                        .set(message)
                        .onConflict(table.TAG_ID, table.SUBMISSION_ID)
                        .doUpdate()
                        .set(table.TAG_ID, excluded.TAG_ID)
                        .returning()
                        .fetch()
                        .into(recordClass)
                        .first()
            }
        }
    }

    suspend override fun handleDelete(message: SubmissionTagRecord): SubmissionTagRecord {
        val id = message.getValue(pk.name)
        log.trace("Delete requested for id = $id in table ${table.name}")

        return db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into(recordClass)
                    .firstOrNull()
                    ?: SubmissionTagRecord().apply{ this.id = id.toString().toInt() }
        }
    }

    @JsonableEventBusConsumerFor(Address.DB.Submission.Tags.Update)
    suspend fun consumeSubmissionTagsUpdate(query: Submission.TagUpdateQuery): JsonObject {
        return dbWithTransactionAsync {
            with(Tables.SUBMISSION_TAG) {
                deleteFrom(this)
                        .where(SUBMISSION_ID.eq(query.submissionId))
                        .executeKAsync()

                val tags = query.tags
                        .fold(
                                insertInto(this).columns(SUBMISSION_ID, TAG_ID)
                        ) { insert, tag ->
                            insert.values(query.submissionId, tag.id)
                        }
                        .returning(TAG_ID)
                        .executeKAsync()

                JsonObject("status" to "success", "tags" to tags)
            }
        }
    }
}
