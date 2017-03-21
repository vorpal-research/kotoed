package org.jetbrains.research.kotoed.teamcity.verticles

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.teamcity.util.DimensionLocator
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.teamcity.util.plus
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.putHeader
import org.jetbrains.research.kotoed.util.vxa
import java.time.Duration

class BuildPollerVerticle(val id: Int) : AbstractVerticle(), Loggable {

    private val ERROR_LIMIT = 5

    override fun start() {
        poll(0)
    }

    fun poll(errorCount: Int) {
        if (errorCount > ERROR_LIMIT) {
            log.trace("Stopping polling for build $id: too many errors")
            return
        }

        val wc = WebClient.create(vertx)

        val idLocator = DimensionLocator.from("id", id)

        launch(UnconfinedWithExceptions { ex ->
            log.trace(ex)
            poll(errorCount + 1)
        }) {
            val response = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.BuildQueue + idLocator)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .send(it)
            }

            val json = response.bodyAsJsonObject()

            if ("finished" == json.getString("state", "none")) {
                log.trace("Build $id finished")
                vertx.undeploy(deploymentID())
            } else {
                vertx.setTimer(Duration.ofSeconds(5).toMillis()) { id ->
                    poll(errorCount)
                }
            }
        }
    }
}
