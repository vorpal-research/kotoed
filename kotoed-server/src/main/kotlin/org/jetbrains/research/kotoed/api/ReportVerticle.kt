package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonObject
import kotlinx.coroutines.withContext
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerStatus
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerTestMethodRun
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerTestRun
import org.jetbrains.research.kotoed.data.statistics.ReportResponse
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import java.time.OffsetDateTime
import java.util.*

data class ReportRequest(val id: Int, val timestamp: OffsetDateTime?) : Jsonable

@AutoDeployable
class ReportVerticle : AbstractKotoedVerticle() {

    private val reportPool = betterFixedThreadPoolContext(4, "report-pool")

    private val template = "results\\.json".toRegex()
    private val successfulStatus = KotoedRunnerStatus.SUCCESSFUL

    private val ignoredTags = listOf("Example")

    private val List<Int>.grade: Int
        get() = this.singleOrNull() ?: 1 // 1 is a default grade

    private val Iterable<String>.onlyNumbers: List<Int>
        get() = this.mapNotNull { it.toIntOrNull() }

    private val Double.fmt get() = String.format(Locale.ROOT, "%.2f", this)
    private fun Double?.orZero() = if (this?.isNaN() == false) this else 0.0

    private fun extractLessonsFromTestResults(content: KotoedRunnerTestRun): Map<String, Map<String, List<KotoedRunnerTestMethodRun>>> {
        return content.data
                .groupBy { it.packageName.split('.').first() }
                .mapValues { (_, results) ->
                    results.groupBy { it.methodName }
                }
    }

    private val TAKE_N_HIGHEST_GRADE_TASKS = 2
    private val TAKE_N_HIGHEST_GRADE_LESSONS = 5

    private fun calcHighestGradesLessons(lessonGrades: List<Pair<String, Int>>) =
            lessonGrades.sortedByDescending { it.second }.filter { it.second > 0.0 }.take(TAKE_N_HIGHEST_GRADE_LESSONS)

    private fun calcHighestGradeTasks(tasks: Map<String, List<KotoedRunnerTestMethodRun>>): List<Pair<String, Int>> {
        val solvableTasks = tasks.filter { it.value.flatMap { it.tags }.intersect(ignoredTags).isEmpty() }
        val successfulTasks = solvableTasks.filterValues { it.flatMap { it.results }.all { it.status == successfulStatus } }
        val taskGrades = successfulTasks.mapValues { it.value.flatMap { it.tags }.onlyNumbers.grade }
        return taskGrades.toList().sortedByDescending { it.second }.take(TAKE_N_HIGHEST_GRADE_TASKS)
    }

    // TODO: Probably incompatible with the new grading system
    private fun calcScore(sr: SubmissionResultRecord): Double {
        if (template !in sr.type) return 0.0

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val groupedLessonData = extractLessonsFromTestResults(content)

        val lessonsGrade = groupedLessonData.map { (lesson, tasks) ->
            val highestGrades = calcHighestGradeTasks(tasks)
            lesson to highestGrades.sumBy { it.second }
        }

        val highestGradesLessons = calcHighestGradesLessons(lessonsGrade)
        val totalScore = highestGradesLessons.sumBy { it.second }

        return totalScore.toDouble() // TODO: Change type to Int everywhere
    }

    private fun calcAllScores(sr: SubmissionResultRecord): List<List<String>> {
        if (template !in sr.type) return listOf()

        val scoreDescriptions: MutableMap<String, String> = mutableMapOf()
        val scores: MutableMap<String, Int> = mutableMapOf()

        val content: KotoedRunnerTestRun = (sr.body as JsonObject).snakeKeys().toJsonable()

        val groupedLessonData = extractLessonsFromTestResults(content)

        groupedLessonData.forEach { (lesson, tasks) ->
            val highestGrades = calcHighestGradeTasks(tasks)
            scores[lesson] = highestGrades.sumBy { it.second }
            scoreDescriptions[lesson] = highestGrades.joinToString(separator = " + ") { "${it.second} points for ${it.first}" }
        }

        val highestGradesLessons = calcHighestGradesLessons(scores.toList())
        val totalScore = highestGradesLessons.sumBy { it.second }
        val totalScoreDescription = highestGradesLessons.joinToString(separator = " + ") { it.first }

        val header = listOf(listOf("", "Score", "Description"))
        val lessons = groupedLessonData.keys
        val data = lessons.sorted().mapIndexed { _, lesson ->
            listOf(lesson, scores[lesson]?.toString() ?: "", scoreDescriptions[lesson] ?: "")
        }
        val footer = listOf(listOf("Total", totalScore.toString(), totalScoreDescription))

        return header + data + footer
    }

    suspend fun makeReport(request: ReportRequest, subStates: List<String>): Map<String, Pair<Double, Double>> {
        val date = request.timestamp ?: OffsetDateTime.now()

        return dbFindAsync(DenizenRecord()).map { denizen -> async(reportPool) iter@{
            val result = dbQueryAsync(Tables.SUBMISSION_RESULT) {
                join(Tables.SUBMISSION) {
                    join(Tables.PROJECT) {
                        join(Tables.DENIZEN, field = "denizen_id") {
                            find { id = denizen.id }
                        }
                    }

                    rjoin(Tables.SUBMISSION_TAG, resultField = "tags") {
                        join(Tables.TAG)
                    }
                }
                filter("(submission.project.course_id == ${request.id}) and " +
                        "(" + subStates.map { "submission.state == \"$it\"" }.joinToString(" or ") + ") and " +
                        "type == \"results.json\"")
            }.sortedByDescending { (it.safeNav("submission") as JsonObject).toRecord<SubmissionRecord>().datetime }
                    .dropWhile { (it.safeNav("submission") as JsonObject).toRecord<SubmissionRecord>().datetime > date }
                    .firstOrNull()

            val rec = result?.toRecord<SubmissionResultRecord>()?.let(this::calcScore)
                    ?: return@iter null
            val tag = result
                    .getJsonObject("submission")
                    ?.getJsonArray("tags")
                    ?.mapNotNull { (it as? JsonObject).safeNav("tag", "name")?.toString()?.toDoubleOrNull() }
                    ?.firstOrNull()
                    ?: 0.75

            denizen.denizenId to (rec to tag)
        }}
                .mapNotNull { it.await() }
                .toMap()
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Report)
    suspend fun handleReport(request: ReportRequest): ReportResponse = withContext(reportPool) {
        val open = makeReport(request, listOf("open", "obsolete", "closed"))
        val closed = makeReport(request, listOf("closed"))

        val students = open.keys + closed.keys
        val result = listOf(
                listOf("Student", "Score (all)", "Adjustment", "Score (closed)")
        ) + students.sorted().map {
            listOf(
                    it,
                    open[it]?.first.orZero().fmt,
                    (open[it]?.second ?: 0.75).fmt,
                    closed[it]?.first.orZero().fmt
            )
        }

        ReportResponse(result)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Report)
    suspend fun handleSubmission(record: ReportRequest): ReportResponse = withContext(reportPool) {
        val result = dbFindAsync(SubmissionResultRecord().apply { submissionId = record.id }).firstOrNull { template in it.type }

        result ?: return@withContext ReportResponse(listOf())

        ReportResponse (calcAllScores(result))
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Result.Report)
    suspend fun handleSubmissionResult(record: ReportRequest): ReportResponse = withContext(reportPool) {
        val result = dbFindAsync(SubmissionResultRecord().apply { id = record.id }).firstOrNull { template in it.type }

        result ?: return@withContext ReportResponse(listOf())

        ReportResponse (calcAllScores(result))
    }

}
