package org.jetbrains.research.kotoed.buildbot.verticles

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.buildbot.util.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildbot.build.BuildRequestInfo
import org.jetbrains.research.kotoed.data.buildbot.build.TriggerBuild
import org.jetbrains.research.kotoed.data.buildbot.project.CreateProject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.util.*

@AutoDeployable
class BuildbotVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Buildbot.Project.Create)
    suspend fun consumeProjectCreate(projectCreate: CreateProject): JsonObject {

        val wc = WebClient.create(vertx)

        val endpointLocator = StringLocator(
                Kotoed2Buildbot.courseName2endpoint(projectCreate.courseName))

        val res = vxa<HttpResponse<Buffer>> {
            wc.post(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Empty + endpointLocator)
                    .putDefaultBBHeaders()
                    .sendForm(
                            mapOf(
                                    "name" to projectCreate.name,
                                    "url" to projectCreate.repoUrl,
                                    "type" to projectCreate.repoType
                            ).asMultiMap(),
                            it
                    )
        }

        if (res.statusCode() == HttpResponseStatus.OK.code()) {
            return JsonObject(
                    "result" to "success"
            )

        } else {
            throw KotoedException(
                    res.statusCode(),
                    res.bodyAsString()
            )
        }
    }

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.Trigger)
    suspend fun consumeBuildTrigger(buildTrigger: TriggerBuild): JsonObject {

        val wc = WebClient.create(vertx)

        val uuid = UUID.randomUUID()

        val res = vxa<HttpResponse<Buffer>> {
            wc.post(Config.Buildbot.Port, Config.Buildbot.Host,
                    BuildbotApi.ForceSchedulers + StringLocator(buildTrigger.schedulerId))
                    .putDefaultBBHeaders()
                    .sendJson(
                            mapOf(
                                    "jsonrpc" to "2.0",
                                    "method" to "force",
                                    "params" to mapOf(
                                            "revision" to buildTrigger.revision
                                    ),
                                    "id" to uuid
                            ),
                            it
                    )
        }

        if (res.statusCode() == HttpResponseStatus.OK.code()) {
            val buildRequestId = res.bodyAsJsonObject().getJsonArray("result")?.let {
                val buildRequestId = it[0] as? Int

                buildRequestId?.also {
                    log.info("Starting polling for build request id: $it")
                    vertx.deployVerticle(BuildRequestPollerVerticle(it))
                }

            } ?: throw KotoedException(
                    HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                    "Unexpected response from Buildbot: ${res.bodyAsString()}"
            )

            return JsonObject(
                    "result" to "success",
                    "build_request_id" to buildRequestId
            )

        } else {
            throw KotoedException(
                    res.statusCode(),
                    res.bodyAsString()
            )
        }
    }

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.RequestInfo)
    suspend fun consumeBuildRequestInfo(buildRequestInfo: BuildRequestInfo): JsonObject {

        val wc = WebClient.create(vertx)

        val res = vxa<HttpResponse<Buffer>> {
            wc.get(Config.Buildbot.Port, Config.Buildbot.Host,
                    BuildbotApi.BuildRequests + IntLocator(buildRequestInfo.buildRequestId))
                    .putDefaultBBHeaders()
                    .send(it)
        }

        if (res.statusCode() == HttpResponseStatus.OK.code()) {
            return JsonObject(
                    "result" to "success"
            ).mergeIn(res.bodyAsJsonObject())

        } else {
            throw KotoedException(
                    res.statusCode(),
                    res.bodyAsString()
            )
        }
    }

}
