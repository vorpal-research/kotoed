package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.DbRecordListWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.Loggable

@AutoDeployable
class SubmissionResultVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Submission.Result.Read)
    suspend fun handleRead(submission: SubmissionRecord): DbRecordListWrapper {
        val sub: SubmissionRecord = dbFetchAsync(submission)
        val status: VerificationData = dbProcessAsync(sub)

        when (status.status) {
            VerificationStatus.Processed -> {
                val results: List<SubmissionResultRecord> = dbFindAsync(
                        SubmissionResultRecord().setSubmissionId(sub.id)
                )

                return DbRecordListWrapper(results, status)
            }
            else -> {
                return DbRecordListWrapper(emptyList(), status)
            }
        }
    }

}
