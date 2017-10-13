package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.Submission
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionTagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.NotFound
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.into

@AutoDeployable
class SubmissionTagVerticle : CrudDatabaseVerticleWithReferences<SubmissionTagRecord>(Tables.SUBMISSION_TAG) {

    override suspend fun handleCreate(message: SubmissionTagRecord): SubmissionTagRecord {
        log.trace("Create requested in table ${table.name}:\n$message")

        for (field in table.primaryKey.fieldsArray) {
            message.reset(field)
        }

        return db {
            sqlStateAware {
                insertInto(table)
                        .set(message)
                        .onDuplicateKeyIgnore()
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
