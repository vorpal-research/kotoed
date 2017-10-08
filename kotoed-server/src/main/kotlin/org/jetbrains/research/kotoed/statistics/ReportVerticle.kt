package org.jetbrains.research.kotoed.statistics

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import java.time.OffsetDateTime

data class ReportRequest(val date: OffsetDateTime?): Jsonable
data class ReportReply(val open: List<Pair<String, Int>>, val closed: List<Pair<String, Int>>): Jsonable

@AutoDeployable
class ReportVerticle: AbstractKotoedVerticle() {

    private val template = "results\\.json".toRegex()
    private val successfulStatus = "SUCCESSFUL"

    private val hardness = compareByDescending<String> {
        when(it) {
            "Impossible" -> 5
            "Hard" -> 4
            "Normal" -> 3
            "Easy" -> 2
            "Trivial" -> 1
            else -> 0
        }
    }

    private fun calcScore(sr: SubmissionResultRecord): Int {
        if(template !in sr.type) return 0

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val lessonData =
                content.data
                .groupBy { it.packageName.split('.').first() }
                .mapValues { (_, results) ->
                    results.filter { it.results.all { it.status == successfulStatus } }
                            .flatMap { it.tags }
                            .sortedWith(hardness)
                }

        var total = 0

        for((_, data) in lessonData) {

            var lessonTotal = 0
            val (first, second, third) = data + listOf("", "", "")

            when (first) {
                "Impossible" -> lessonTotal += 8
                "Hard" -> lessonTotal += 5
                "Normal" -> lessonTotal += 2
                "Easy", "Trivial" -> lessonTotal += 1
            }

            when (second) {
                "Impossible" -> lessonTotal += 3
                "Hard" -> lessonTotal += 2
                "Normal", "Easy" -> lessonTotal += 1
            }

            when (third) {
                "Impossible", "Hard", "Normal" -> lessonTotal += 1
            }

            total += lessonTotal
        }

        return total
    }


    suspend fun makeReport(request: ReportRequest, subStatuses: List<String>): List<Pair<String, Int>> {
        val date = request.date ?: OffsetDateTime.now()

        val q = ComplexDatabaseQuery(Tables.SUBMISSION_RESULT)
                .join(
                        ComplexDatabaseQuery(Tables.SUBMISSION)
                                .join(
                                        ComplexDatabaseQuery(Tables.PROJECT)
                                                .join("denizen", "denizen_id")
                                )
                ).filter(subStatuses.map{ "submission.status == \"$it\"" }.joinToString(" or "))

        val resp = dbQueryAsync(q)

        return resp
                .groupBy { it.safeNav("submission", "project", "denizen", "denizenId") as? String }
                .map { (k, v) ->
                    k to v.filter {
                        (it.safeNav("submission") as JsonObject).toRecord<SubmissionRecord>().datetime <= date
                    }.map {
                        it.toRecord<SubmissionResultRecord>()
                    }
                }
                .map { (k, v) -> k.orEmpty() to (v.map(this::calcScore).max() ?: 0) }
                .sortedBy { it.first }
    }

    @JsonableEventBusConsumerFor(Address.Api.Report)
    suspend fun handleReport(request: ReportRequest): ReportReply {
        val open = makeReport(request, listOf("open", "closed"))
        val closed = makeReport(request, listOf("closed"))
        return ReportReply(open, closed)
    }

}