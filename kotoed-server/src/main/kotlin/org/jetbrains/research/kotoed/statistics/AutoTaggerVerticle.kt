package org.jetbrains.research.kotoed.statistics

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.buildSystem.BuildResponse
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionTagRecord
import org.jetbrains.research.kotoed.database.tables.records.TagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.time.OffsetDateTime

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

    override fun start(startFuture: Future<Void>) {
        vertx.setPeriodic(java.time.Duration.ofHours(2).toMillis()) { handleHeartbeat() }
        super.start(startFuture)
    }

    fun byNameLazy(name: String) =
            coroLazy { dbFindAsync(TagRecord().apply { this.name = name }).first().id!! }

    var buildFailedId_ = byNameLazy("build failed")
    suspend fun getBuildFailed() = buildFailedId_.get()

    var testsFailedId_ = byNameLazy("tests failed")
    suspend fun getTestsFailed() = testsFailedId_.get()

    var badStyleId_ = byNameLazy("bad style")
    suspend fun getBadStyle() = badStyleId_.get()

    var buildOkId_ = byNameLazy("build ok")
    suspend fun getBuildOk() = buildOkId_.get()

    var emptySubId_ = byNameLazy("empty")
    suspend fun getEmptySub() = emptySubId_.get()

    var checkMeId_ = byNameLazy("check me")
    suspend fun getCheckMe() = checkMeId_.get()

    val staleId_ = byNameLazy("stale")
    suspend fun getStale() = staleId_.get()

    suspend fun setTag(submissionId: Int, tagId: Int): Unit =
            try {
                sendJsonableAsync(Address.Api.Submission.Tags.Create,
                        SubmissionTagRecord().apply { this.submissionId = submissionId; this.tagId = tagId })
            } catch (ex: Exception) {
                log.warn("Could not set tag: $tagId on $submissionId")
            }

    suspend fun removeTag(submissionId: Int, tagId: Int): Unit =
            try {
                sendJsonableAsync(Address.Api.Submission.Tags.Delete,
                        SubmissionTagRecord().apply { this.submissionId = submissionId; this.tagId = tagId })
            } catch (ex: Exception) {
                log.warn("Could not remove tag: $tagId on $submissionId")
            }

    private val successTemplate = "results\\.json".toRegex()

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Result)
    suspend fun consumeBuildResult(build: BuildResponse) = when(build) {
        is BuildResponse.BuildInspection -> {
            // TODO: Inspection-related tags?
        }
        is BuildResponse.BuildSuccess -> {
            removeTag(build.submissionId, getStale())
            removeTag(build.submissionId, getBuildFailed())
            removeTag(build.submissionId, getTestsFailed())
            removeTag(build.submissionId, getBuildOk())
            removeTag(build.submissionId, getEmptySub())

            val content: KotoedRunnerTestRun = build.results.snakeKeys().toJsonable()

            if(content.data.any { it.results.any {
                        it.status != "SUCCESSFUL" &&
                                it.failure != null &&
                                !it.failure.nestedException.startsWith("kotlin.NotImplementedError")
                    } }) {
                setTag(build.submissionId, getTestsFailed())
            } else {
                if(content.data.all {
                            it.results.any { it.status != "SUCCESSFUL" } || "Example" in it.tags
                        }) {
                    setTag(build.submissionId, getEmptySub())
                } else {
                    setTag(build.submissionId, getBuildOk())
                }
            }
        }
        is BuildResponse.BuildFailed -> {
            removeTag(build.submissionId, getStale())
            removeTag(build.submissionId, getBuildFailed())
            removeTag(build.submissionId, getTestsFailed())
            removeTag(build.submissionId, getBuildOk())
            removeTag(build.submissionId, getEmptySub())

            setTag(build.submissionId, getBuildFailed())
        }
    }

    @JsonableEventBusConsumerFor(Address.Event.Submission.Created)
    suspend fun consumeSubmissionCreated(sub: SubmissionRecord) {
        if (sub.parentSubmissionId == null) {
            setTag(sub.id, getCheckMe())
        }
    }

    fun handleHeartbeat() =
        spawn {
            val allSubs = dbFindAsync(SubmissionRecord().apply { state = SubmissionState.open })
            for(sub in allSubs) {
                if(sub.datetime < OffsetDateTime.now().minusWeeks(2)) {
                    setTag(sub.id, getStale())
                } else {
                    removeTag(sub.id, getStale())
                }
            }
        }
}
