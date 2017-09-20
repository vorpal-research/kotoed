package org.jetbrains.research.kotoed.buildbot.verticles

import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.buildbot.util.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildbot.build.BuildCrawl
import org.jetbrains.research.kotoed.data.notification.NotificationType
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import java.time.Duration

data class BuildsResponse(
        val builds: List<Build>
) : Jsonable

data class Build(
        val builderid: Int,
        val buildid: Int,
        val buildrequestid: Int,
        val complete: Boolean
) : Jsonable


class BuildRequestPollerVerticle(val buildRequestId: Int) : AbstractKotoedVerticle(), Loggable {

    // FIXME akhin Stale verticle detection?

    // FIXME akhin Move to config

    private val ERROR_LIMIT = 5
    private val RETRY_PERIOD = 5L // seconds

    override fun start() {
        poll(0, mutableSetOf())
    }

    fun retry(errorCount: Int, processedBuildIds: MutableSet<Int>) {
        vertx.setTimer(Duration.ofSeconds(RETRY_PERIOD).toMillis()) {
            poll(errorCount, processedBuildIds)
        }
    }

    fun poll(errorCount: Int, processedBuildIds: MutableSet<Int>) {
        if (errorCount > ERROR_LIMIT) {
            log.trace("Stopped polling for build request $buildRequestId: too many errors")
            vertx.undeploy(deploymentID())
            return
        }

        val eb = vertx.eventBus()

        val wc = WebClient.create(vertx)

        val buildRequestLocator = DimensionLocator.from("buildrequests", buildRequestId) /
                StringLocator("builds")

        spawn(WithExceptions { ex ->
            log.trace("Error when polling build request", ex)
            poll(errorCount + 1, processedBuildIds)
        }) {
            val response = vxa<HttpResponse<Buffer>> {
                wc.get(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Root + buildRequestLocator)
                        .putDefaultBBHeaders()
                        .send(it)
            }

            val buildRequestsResponse = fromJson<BuildsResponse>(response.bodyAsJsonObject())

            val builds = buildRequestsResponse.builds

            for (build in builds.filterNot { it.buildid in processedBuildIds }) {
                if (build.complete) {
                    log.trace("Build ${build.buildid} finished for $buildRequestId")

                    publishJsonable(
                            Address.Buildbot.Build.BuildCrawl,
                            BuildCrawl(buildRequestId, build.buildid)
                    )

                    processedBuildIds += build.buildid
                }
            }

            if (buildRequestsResponse.builds.isNotEmpty() &&
                    processedBuildIds.size == buildRequestsResponse.builds.size) {

                // Notify about the results

                // FIXME akhin: Use ComplexDatabaseQuery instead of this shit

                val build = dbFindAsync(
                        BuildRecord().also { it.buildRequestId = buildRequestId })[0]
                val sub = dbFetchAsync(
                        SubmissionRecord().apply { id = build.submissionId })
                val project = dbFetchAsync(
                        ProjectRecord().apply { id = sub.projectId });

                createNotification(
                        NotificationRecord().apply {
                            type = NotificationType.NEW_SUBMISSION_RESULTS.name
                            denizenId = project.denizenId
                            body = build.toJson()
                        }
                )

                log.trace("I'm done here! $buildRequestId")
                vertx.undeploy(deploymentID())

            } else {
                log.trace("Poll me baby one more time! $buildRequestId")
                retry(errorCount, processedBuildIds)
            }
        }
    }
}
