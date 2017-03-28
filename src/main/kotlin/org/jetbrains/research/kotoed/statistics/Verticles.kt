package org.jetbrains.research.kotoed.statistics

import io.netty.handler.codec.http.HttpHeaderNames
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.build.ArtifactContent
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.io.ByteArrayInputStream
import java.io.InputStream

class JUnitStatisticsVerticle : AbstractVerticle(), Loggable {

    private val template = "TEST-.*\\.xml".toRegex()

    private val handlers = listOf(
            JUnit::totalTestCount,
            JUnit::failedTestCount
    )

    override fun start(startFuture: Future<Void>) {
        val eb = vertx.eventBus()

        eb.consumer(
                Address.TeamCity.Build.Artifact,
                this@JUnitStatisticsVerticle::consumeTeamCityArtifact.withExceptions()
        )

        startFuture.complete()
    }

    fun consumeTeamCityArtifact(msg: Message<JsonObject>) {
        val wc = WebClient.create(vertx)

        val artifactContent = fromJson<ArtifactContent>(msg.body())

        if (template !in artifactContent.path) return

        log.trace("Processing artifact ${artifactContent.path}")

        launch(UnconfinedWithExceptions(msg)) {
            val artifactContentRes = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, artifactContent.path)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .send(it)
            }

            val xmlStream = ByteArrayInputStream(artifactContentRes.body().bytes)

            log.trace(xml2json(xmlStream)).also { xmlStream.reset() }

            for (handler in handlers) {
                log.trace(handler.name)
                log.trace(handler(xmlStream)).also { xmlStream.reset() }
            }
        }
    }
}

internal object JUnit {
    fun totalTestCount(xmlStream: InputStream): String {
        val nodes = evaluateXPath("/testsuite/@tests", xmlStream)
        assert(1 == nodes.size)
        return nodes.first().nodeValue
    }

    fun failedTestCount(xmlStream: InputStream): String {
        val nodes = evaluateXPath("/testsuite/@failures", xmlStream)
        assert(1 == nodes.size)
        return nodes.first().nodeValue
    }
}
