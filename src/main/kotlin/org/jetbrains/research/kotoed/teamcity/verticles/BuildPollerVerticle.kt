package org.jetbrains.research.kotoed.teamcity.verticles

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.build.ArtifactCrawl
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.DimensionLocator
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.teamcity.util.plus
import org.jetbrains.research.kotoed.teamcity.util.putDefaultTCHeaders
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.vxa
import java.time.Duration

class BuildPollerVerticle(val id: Int) : AbstractVerticle(), Loggable {

    // FIXME akhin Stale verticle detection?

    // FIXME akhin Move to config

    private val ERROR_LIMIT = 5
    private val RETRY_PERIOD = 5L // seconds

    override fun start() {
        poll(0)
    }

    fun retry(errorCount: Int) {
        vertx.setTimer(Duration.ofSeconds(RETRY_PERIOD).toMillis()) {
            poll(errorCount)
        }
    }

    fun poll(errorCount: Int) {
        if (errorCount > ERROR_LIMIT) {
            log.trace("Stopping polling for build $id: too many errors")
            vertx.undeploy(deploymentID())
            return
        }

        val eb = vertx.eventBus()
        val wc = WebClient.create(vertx)

        val idLocator = DimensionLocator.from("id", id)

        launch(UnconfinedWithExceptions { ex ->
            log.trace(ex)
            poll(errorCount + 1)
        }) {
            val response = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.BuildQueue + idLocator)
                        .putDefaultTCHeaders()
                        .send(it)
            }

            val json = response.bodyAsJsonObject()

            if ("finished" == json.getString("state", "none")) {
                log.trace("Build $id finished")

                eb.publish(
                        Address.TeamCity.Build.Crawl,
                        ArtifactCrawl(
                                json.getJsonObject("artifacts")
                                        .getString("href")
                        )
                )

                vertx.undeploy(deploymentID())
            } else {
                retry(errorCount)
            }
        }
    }
}
