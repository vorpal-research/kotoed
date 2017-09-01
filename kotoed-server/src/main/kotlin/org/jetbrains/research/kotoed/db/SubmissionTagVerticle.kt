package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.Submission
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionTagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.database.executeKAsync

@AutoDeployable
class SubmissionTagVerticle : CrudDatabaseVerticleWithReferences<SubmissionTagRecord>(Tables.SUBMISSION_TAG) {
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
