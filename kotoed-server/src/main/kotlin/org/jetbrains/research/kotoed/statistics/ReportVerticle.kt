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

data class ReportRequest(val date: OffsetDateTime?) : Jsonable
data class ReportReply(val open: List<Pair<String, Double>>, val closed: List<Pair<String, Double>>) : Jsonable

@AutoDeployable
class ReportVerticle : AbstractKotoedVerticle() {

    private val template = "results\\.json".toRegex()
    private val successfulStatus = "SUCCESSFUL"

    private val String?.grade: Double
        get() = when (this) {
            "Impossible" -> 10.0
            "Hard" -> 7.0
            "Normal" -> 4.0
            "Easy" -> 1.0
            "Trivial" -> 0.0
            else -> 0.0
        }

    private val List<Double>.grade: Double
        get() = when (size) {
            0 -> 0.0
            1 -> this[0]
            else -> {
                val (first, second) = this
                if (first == second) first + 1.0 else first
            }
        }

    private fun calcScore(sr: SubmissionResultRecord): Double {
        if (template !in sr.type) return 0.0

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val goodLessonData =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .filter { (_, v) -> v.flatMap { it.results }.all { it.status == successfulStatus } }
                                    .map { (_, v) -> v.flatMap { it.tags }.firstOrNull().grade }
                                    .sortedDescending()
                        }

        val fullLessonData =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .map { (_, v) -> v.flatMap { it.tags }.firstOrNull().grade }
                                    .sortedDescending()
                        }

        val lessonData: Map<String, Pair<List<Double>, List<Double>>> =
                fullLessonData.entries.fold(mutableMapOf()) { acc, (id, fullData) ->
                    acc.also {
                        it[id] = (goodLessonData[id] ?: emptyList()) to fullData
                    }
                }

        var total = 0.0

        for ((_, data) in lessonData) {
            val (goodData, fullData) = data
            val score = if (0.0 == fullData.grade) 0.0 else goodData.grade / fullData.grade
            total += score
        }

        return total
    }


    suspend fun makeReport(request: ReportRequest, subStates: List<String>): List<Pair<String, Double>> {
        val date = request.date ?: OffsetDateTime.now()

        val q = ComplexDatabaseQuery(Tables.SUBMISSION_RESULT)
                .join(
                        ComplexDatabaseQuery(Tables.SUBMISSION)
                                .join(
                                        ComplexDatabaseQuery(Tables.PROJECT)
                                                .join("denizen", "denizen_id")
                                )
                ).filter(subStates.map { "submission.state == \"$it\"" }.joinToString(" or "))

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
                .map { (k, v) ->
                    k.orEmpty() to (v.map(this::calcScore).max() ?: 0.0)
                }
                .sortedBy { it.first }
    }

    @JsonableEventBusConsumerFor(Address.Api.Report)
    suspend fun handleReport(request: ReportRequest): ReportReply {
        val open = makeReport(request, listOf("open", "closed"))
        val closed = makeReport(request, listOf("closed"))
        return ReportReply(open, closed)
    }

}
