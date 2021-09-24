package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import org.jetbrains.research.kotoed.data.api.DbRecordListWrapper
import org.jetbrains.research.kotoed.data.api.SearchQueryWithTags
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.setPageForQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@AutoDeployable
class SubmissionResultVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Submission.Result.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordListWrapper {
        val sub: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(sub)

        return when (status.status) {
            VerificationStatus.Processed -> {
                val results: List<SubmissionResultRecord> = dbFindAsync(
                        SubmissionResultRecord().setSubmissionId(sub.id)
                )

                DbRecordListWrapper(results, status)
            }
            else -> {
                DbRecordListWrapper(emptyList(), status)
            }
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Result.BatchRead)
    suspend fun handleBatchRead(query: SearchQueryWithTags): JsonArray {
        val groupId = query.find?.getString("group_id")

        val profileQ = ComplexDatabaseQuery(Tables.PROFILE)
                .join(Tables.DENIZEN)

        val profiles = dbQueryAsync(
                groupId?.let { profileQ.filter("group_id == %s".formatToQuery(groupId)) }
                        ?: profileQ
        )

        val denizens = profiles.map {
            it.getJsonObject("denizen")?.getInteger("id") ?: -1
        }.toSet()

        // FIXME: akhin Find the last submission? The last successful submission?

        val q = ComplexDatabaseQuery(Tables.SUBMISSION_RESULT).join(
                ComplexDatabaseQuery(Tables.SUBMISSION).join(
                        ComplexDatabaseQuery(Tables.PROJECT).join(
                                Tables.DENIZEN)))
                .filter("submission.project.denizen_id in %s and submission.state == %s".formatToQuery(denizens, SubmissionState.open))
                .setPageForQuery(query)

        val req = dbQueryAsync(q)

        return JsonArray(req)
    }

}
