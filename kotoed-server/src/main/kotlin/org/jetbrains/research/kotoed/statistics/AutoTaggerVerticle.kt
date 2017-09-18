package org.jetbrains.research.kotoed.statistics

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionTagRecord
import org.jetbrains.research.kotoed.database.tables.records.TagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

data class KotoedRunnerFailure(
        val nestedException: String
): Jsonable
data class KotoedRunnerTestResult(
        val status: String,
        val failure: KotoedRunnerFailure?
): Jsonable
data class KotoedRunnerTestMethodRun(
        val tags: List<String>,
        val results: List<KotoedRunnerTestResult>,
        val methodName: String,
        val packageName: String
): Jsonable
data class KotoedRunnerTestRun(val data: List<KotoedRunnerTestMethodRun>): Jsonable

@AutoDeployable
class AutoTaggerVerticle: AbstractKotoedVerticle() {

    var buildFailedId_: Int? = null
    suspend fun getBuildFailed(): Int =
            buildFailedId_ ?: dbFindAsync(TagRecord().apply { name = "build failed" }).first().id

    var testsFailedId_: Int? = null
    suspend fun getTestsFailed(): Int =
            testsFailedId_ ?: dbFindAsync(TagRecord().apply { name = "tests failed" }).first().id

    var badStyleId_: Int? = null
    suspend fun getBadStyle(): Int =
            badStyleId_ ?: dbFindAsync(TagRecord().apply { name = "bad style" }).first().id

    var buildOkId_: Int? = null
    suspend fun getBuildOk(): Int =
            buildOkId_ ?: dbFindAsync(TagRecord().apply { name = "build ok" }).first().id

    suspend fun setTag(submissionId: Int, tagId: Int): Unit =
            sendJsonableAsync(Address.Api.Submission.Tags.Create,
                    SubmissionTagRecord().apply { this.submissionId = submissionId; this.tagId = tagId })

    suspend fun removeTag(submissionId: Int, tagId: Int): Unit =
            sendJsonableAsync(Address.Api.Submission.Tags.Delete,
                    SubmissionTagRecord().apply { this.submissionId = submissionId; this.tagId = tagId })

    private val successTemplate = "results\\.json".toRegex()

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.LogContent)
    suspend fun consumeLogContent(logContent: LogContent) {
        val build = dbFindAsync(BuildRecord().setBuildRequestId(logContent.buildRequestId()))
                .firstOrNull() ?: throw IllegalStateException(
                "Build request ${logContent.buildRequestId()} not found")

        removeTag(build.submissionId, getBuildFailed())
        removeTag(build.submissionId, getTestsFailed())
        removeTag(build.submissionId, getBuildOk())
        //removeTag(build.submissionId, getBadStyle())

        if(0 != logContent.results()) {
            // build error
            setTag(build.submissionId, getBuildFailed())
        }

        if (successTemplate in logContent.logName()) {
            val content: KotoedRunnerTestRun = JsonObject(logContent.content).snakeKeys().toJsonable()

            if(content.data.any { it.results.any {
                it.status != "SUCCESSFUL" &&
                        it.failure != null &&
                        !it.failure.nestedException.startsWith("kotlin.NotImplementedError")
            } }) {
                setTag(build.submissionId, getTestsFailed())
            } else {
                setTag(build.submissionId, getBuildOk())
            }

        }
    }
}