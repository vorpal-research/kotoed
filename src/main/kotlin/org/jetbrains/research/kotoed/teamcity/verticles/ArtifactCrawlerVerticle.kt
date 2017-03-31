package org.jetbrains.research.kotoed.teamcity.verticles

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.build.ArtifactContent
import org.jetbrains.research.kotoed.data.teamcity.build.ArtifactCrawl
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.putDefaultTCHeaders
import org.jetbrains.research.kotoed.util.*

internal data class ArtifactData(
        val file: List<ArtifactFile>
) : Jsonable

internal data class ArtifactFile(
        val content: ArtifactHref?,
        val children: ArtifactHref?
) : Jsonable

internal data class ArtifactHref(
        val href: String
) : Jsonable

class ArtifactCrawlerVerticle : AbstractVerticle(), Loggable {
    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                Address.TeamCity.Build.Crawl,
                this@ArtifactCrawlerVerticle::consumeArtifactCrawl.withExceptions()
        )
    }

    fun consumeArtifactCrawl(msg: Message<JsonObject>) {
        val eb = vertx.eventBus()

        val wc = WebClient.create(vertx)

        launch(UnconfinedWithExceptions({ log.error(it) })) {
            val artifactCrawl = fromJson<ArtifactCrawl>(msg.body())

            val artifactCrawlRes = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, artifactCrawl.path)
                        .putDefaultTCHeaders()
                        .send(it)
            }

            if (HttpResponseStatus.OK.code() == artifactCrawlRes.statusCode()) {
                val artifactData = fromJson<ArtifactData>(artifactCrawlRes.bodyAsJsonObject())

                for ((content, children) in artifactData.file) {
                    if (content != null) {
                        log.trace("New build result: ${content.href}")

                        eb.publish(
                                Address.TeamCity.Build.Artifact,
                                ArtifactContent(content.href)
                        )
                    }

                    if (children != null) {
                        eb.publish(
                                Address.TeamCity.Build.Crawl,
                                ArtifactCrawl(children.href)
                        )
                    }
                }
            }
        }
    }
}
