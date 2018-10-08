package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerStatus
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerTestRun
import org.jetbrains.research.kotoed.data.statistics.ReportResponse
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import java.time.OffsetDateTime

data class ReportRequest(val id: Int, val timestamp: OffsetDateTime?) : Jsonable

@AutoDeployable
class ReportVerticle : AbstractKotoedVerticle() {

    private val template = "results\\.json".toRegex()
    private val successfulStatus = KotoedRunnerStatus.SUCCESSFUL

    private val tagging = mapOf(
            "Example" to null,
            "Trivial" to 0.0,
            "Easy" to 1.0,
            "Normal" to 4.0,
            "Hard" to 7.0,
            "Impossible" to 10.0
    )
    private val tags = tagging.entries.sortedBy { it.value }.map { it.key }

    private val String?.grade: Double?
        get() = tagging[this]

    private val List<Double>.grade: Double
        get() = when (size) {
            0 -> 0.0
            1 -> this[0]
            else -> {
                val (first, second) = this
                if (first == second) first + 1.0 else first
            }
        }

    private val Double.fmt get() = String.format("%.2f", this)
    private fun Double?.orZero() = this ?: 0.0

    private fun calcScore(sr: SubmissionResultRecord): Double {
        if (template !in sr.type) return 0.0

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val goodLessonData: Map<String, List<Double>> =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .filter { (_, v) -> v.flatMap { it.results }.all { it.status == successfulStatus } }
                                    .mapNotNull { (_, v) -> v.flatMap { it.tags }.firstOrNull().grade }
                                    .sortedDescending()
                        }

        log.debug("goodLessonData = ${goodLessonData}")

        val fullLessonData: Map<String, List<Double>> =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .mapNotNull { (_, v) -> v.flatMap { it.tags }.firstOrNull().grade }
                                    .sortedDescending()
                        }

        log.debug("fullLessonData = ${fullLessonData}")

        val lessonData: Map<String, Pair<List<Double>, List<Double>>> =
                fullLessonData.entries.fold(mutableMapOf()) { acc, (id, fullData) ->
                    acc.also {
                        it[id] = (goodLessonData[id] ?: emptyList()) to fullData
                    }
                }

        log.debug("lessonData = ${lessonData}")

        var total = 0.0

        for ((_, data) in lessonData) {
            val (goodData, fullData) = data
            val score = if (0.0 == fullData.grade) 0.0 else goodData.grade / fullData.grade
            total += score
        }
        total /= lessonData.size

        log.debug("total = ${total}")

        return total
    }

    private fun calcAllScores(sr: SubmissionResultRecord): List<List<String>> {
        if (template !in sr.type) return listOf()

        val tableData: MutableMap<Pair<String, String>, String> = mutableMapOf()
        val scores: MutableMap<String, Double> = mutableMapOf()

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val goodLessonData: Map<String, List<String>> =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .filter { (_, v) -> v.flatMap { it.results }.all { it.status == successfulStatus } }
                                    .mapNotNull { (_, v) -> v.flatMap { it.tags }.firstOrNull() }
                                    .sortedByDescending { it.grade }
                        }

        val fullLessonData: Map<String, List<String>> =
                content.data
                        .groupBy { it.packageName.split('.').first() }
                        .mapValues { (_, results) ->
                            results.groupBy { it.methodName }
                                    .mapNotNull { (_, v) -> v.flatMap { it.tags }.firstOrNull() }
                                    .sortedByDescending { it.grade }
                        }

        fullLessonData.keys.forEach { lesson ->
            val goodLesson = goodLessonData[lesson].orEmpty()
            val fullLesson = fullLessonData[lesson].orEmpty()
            tags.forEach { tag ->
                val good = goodLesson.count { it == tag }
                val total = fullLesson.count { it == tag }
                tableData[lesson to tag] = "$good/$total"
            }
            val goodScore = goodLesson.mapNotNull { it.grade }.grade
            val fullScore = fullLesson.mapNotNull { it.grade }.grade
            scores[lesson] = goodScore / fullScore
        }

        val header = listOf(listOf("") + tags + listOf("Score"))
        val data = fullLessonData.keys.sorted().mapIndexed { i, lesson ->
            listOf(lesson) + tags.map { tag -> tableData[lesson to tag] ?: "" } + listOf(scores[lesson].orZero().fmt)
        }
        val footer = listOf(listOf("Total") + tags.map { "" } + scores.values.average().toString())

        return header + data + footer
    }

    suspend fun makeReport(request: ReportRequest, subStates: List<String>): Map<String, Double> {
        val date = request.timestamp ?: OffsetDateTime.now()

        val resp = dbQueryAsync(Tables.SUBMISSION_RESULT) {
            join(Tables.SUBMISSION) {
                join(Tables.PROJECT) {
                    join(Tables.DENIZEN, field = "denizen_id")
                }
            }
            filter("(submission.project.course_id == ${request.id}) and " +
                    "(" + subStates.map { "submission.state == \"$it\"" }.joinToString(" or ") + ")")
        }

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
                }.toMap()
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Report)
    suspend fun handleReport(request: ReportRequest): ReportResponse {
        val open = makeReport(request, listOf("open", "closed"))
        val closed = makeReport(request, listOf("closed"))

        val students = open.keys + closed.keys
        val result = listOf(
                listOf("Student", "Score (open)", "Score (closed)")
        ) + students.sorted().map { listOf(it, open[it].orZero().fmt, closed[it].orZero().fmt) }

        return ReportResponse(result)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Report)
    suspend fun handleSubmission(record: ReportRequest): ReportResponse {
        val result = dbFindAsync(SubmissionResultRecord().apply { submissionId = record.id }).firstOrNull()

        result ?: return ReportResponse(listOf())

        return ReportResponse (calcAllScores(result))
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Result.Report)
    suspend fun handleSubmissionResult(record: ReportRequest): ReportResponse {
        val result = dbFindAsync(SubmissionResultRecord().apply { id = record.id }).firstOrNull()

        result ?: return ReportResponse(listOf())

        return ReportResponse (calcAllScores(result))
    }

}
