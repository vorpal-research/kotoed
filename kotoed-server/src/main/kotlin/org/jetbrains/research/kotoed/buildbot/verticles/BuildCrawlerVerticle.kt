package org.jetbrains.research.kotoed.buildbot.verticles

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.buildbot.util.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildbot.build.BuildCrawl
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.data.buildbot.build.LogCrawl
import org.jetbrains.research.kotoed.data.buildbot.build.StepCrawl
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

data class BuildStepsResponse(
        val steps: List<BuildStep>
) : Jsonable, Sequence<BuildStep> {
    override fun iterator(): Iterator<BuildStep> = steps.iterator()
}

data class BuildStep(
        val name: String,
        val stepid: Int,
        val number: Int,
        val complete: Boolean
) : Jsonable

data class StepLogsResponse(
        val logs: List<StepLog>
) : Jsonable, Sequence<StepLog> {
    override fun iterator(): Iterator<StepLog> = logs.iterator()
}

data class StepLog(
        val name: String,
        val logid: Int,
        val slug: String,
        val complete: Boolean
) : Jsonable

data class LogChunksResponse(
        val logchunks: List<LogChunk>
) : Jsonable, Sequence<LogChunk> {
    override fun iterator(): Iterator<LogChunk> = logchunks.iterator()
}

data class LogChunk(
        val logid: Int,
        val content: String
) : Jsonable


@AutoDeployable
class BuildCrawlerVerticle : AbstractKotoedVerticle(), Loggable {

    private inline fun <
            reified CrawlT : Any,
            reified ResponseT,
            reified ValueT : Any,
            reified NextT : Jsonable
            > crawl(msg: Message<JsonObject>,
                    crossinline locatorGen: (CrawlT) -> Locator,
                    nextAddress: String,
                    crossinline nextCrawl: (CrawlT, ValueT) -> NextT
    ) where
            ResponseT : Any,
            ResponseT : Sequence<ValueT> {
        launch(UnconfinedWithExceptions({ log.error("Error when crawling", it) })) {
            val eb = vertx.eventBus()

            val wc = WebClient.create(vertx)

            val crawl = fromJson<CrawlT>(msg.body())

            val locator = locatorGen(crawl)

            val response = vxa<HttpResponse<Buffer>> {
                wc.get(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Root + locator)
                        .putDefaultBBHeaders()
                        .send(it)
            }

            if (HttpResponseStatus.OK.code() == response.statusCode()) {
                val values = fromJson<ResponseT>(response.bodyAsJsonObject())

                for (value in values) {
                    log.trace("New value: $value")

                    eb.publish(
                            nextAddress,
                            nextCrawl(crawl, value).toJson()
                    )
                }

            } else {
                log.trace("Error when crawling: ${response.bodyAsString()}")
            }
        }
    }

    @EventBusConsumerFor(Address.Buildbot.Build.BuildCrawl)
    fun consumeBuildCrawl(msg: Message<JsonObject>) =
            crawl<BuildCrawl, BuildStepsResponse, BuildStep, StepCrawl>(
                    msg,
                    {
                        DimensionLocator.from("builds", it.buildId) /
                                StringLocator("steps")
                    },
                    Address.Buildbot.Build.StepCrawl,
                    {
                        c, v ->
                        StepCrawl(c.buildId, v.stepid)
                    }
            )

    @EventBusConsumerFor(Address.Buildbot.Build.StepCrawl)
    fun consumeStepCrawl(msg: Message<JsonObject>) =
            crawl<StepCrawl, StepLogsResponse, StepLog, LogCrawl>(
                    msg,
                    {
                        DimensionLocator.from("steps", it.stepId) /
                                StringLocator("logs")
                    },
                    Address.Buildbot.Build.LogCrawl,
                    {
                        c, v ->
                        LogCrawl(c.buildId, c.stepId, v.logid, v.name)
                    }
            )

    @EventBusConsumerFor(Address.Buildbot.Build.LogCrawl)
    fun consumeLogCrawl(msg: Message<JsonObject>) =
            crawl<LogCrawl, LogChunksResponse, LogChunk, LogContent>(
                    msg,
                    {
                        DimensionLocator.from("logs", it.logId) /
                                StringLocator("contents")
                    },
                    Address.Buildbot.Build.LogContent,
                    {
                        c, v ->
                        LogContent(c.buildId, c.stepId, c.logId, c.logName, v.content)
                    }
            )

}
