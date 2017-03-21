package org.jetbrains.research.kotoed.teamcity

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.project.BuildInfo
import org.jetbrains.research.kotoed.data.teamcity.project.CreateProject
import org.jetbrains.research.kotoed.data.teamcity.project.TriggerBuild
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.requests.FreeMarkerTemplateEngineImplEx
import org.jetbrains.research.kotoed.teamcity.util.*
import org.jetbrains.research.kotoed.teamcity.verticles.BuildPollerVerticle
import org.jetbrains.research.kotoed.util.*

class TeamCityVerticle : AbstractVerticle(), Loggable {

    val ftlEngine = FreeMarkerTemplateEngineImplEx()

    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                Address.TeamCity.Project.Create,
                this@TeamCityVerticle::consumeTeamCityProjectCreate.withExceptions()
        )

        eb.consumer<JsonObject>(
                Address.TeamCity.Build.Trigger,
                this@TeamCityVerticle::consumeTeamCityBuildTrigger.withExceptions()
        )

        eb.consumer<JsonObject>(
                Address.TeamCity.Build.Info,
                this@TeamCityVerticle::consumeTeamCityBuildInfo.withExceptions()
        )
    }

    fun consumeTeamCityProjectCreate(msg: Message<JsonObject>) {

        val wc = WebClient.create(vertx)

        val createProject = fromJson<CreateProject>(msg.body())

        launch(UnconfinedWithExceptions(msg)) {

            val projectBody = vxa<Buffer> {
                ftlEngine.render(
                        vertx,
                        "org/jetbrains/research/kotoed/teamcity/requests/createProject.ftl",
                        mapOf("project" to createProject.project),
                        it
                )
            }

            val vcsRootBody = vxa<Buffer> {
                ftlEngine.render(
                        vertx,
                        "org/jetbrains/research/kotoed/teamcity/requests/createHgVcsRoot.ftl",
                        mapOf("vcs" to createProject.vcsRoot),
                        it
                )
            }

            val buildConfigBody = vxa<Buffer> {
                ftlEngine.render(
                        vertx,
                        "org/jetbrains/research/kotoed/teamcity/requests/createBuildConfig.ftl",
                        mapOf(
                                "project" to createProject.project,
                                "vcs" to createProject.vcsRoot,
                                "build" to createProject.buildConfig
                        ),
                        it
                )
            }

            val projectRes = vxa<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Projects())
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(projectBody, it)
            }

            val vcsRootRes = vxa<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.VcsRoots())
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(vcsRootBody, it)
            }

            val buildConfigRes = vxa<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.BuildTypes())
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(buildConfigBody, it)
            }

            val results = listOf(
                    "project" to projectRes,
                    "vcsRoot" to vcsRootRes,
                    "buildConfig" to buildConfigRes
            ).groupBy { it.second.statusCode() == HttpResponseStatus.OK.code() }

            if (results[false] == null) {
                msg.reply(
                        JsonObject(
                                results[true]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsJsonObject() }
                                        + ("result" to "success")
                        )
                )
            } else {
                msg.reply(
                        JsonObject(
                                results[false]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsString() }
                                        + ("result" to "failed")
                        )
                )
            }
        }
    }

    fun consumeTeamCityBuildTrigger(msg: Message<JsonObject>) {

        val wc = WebClient.create(vertx)

        val triggerBuild = fromJson<TriggerBuild>(msg.body())

        launch(UnconfinedWithExceptions(msg)) {
            val triggerBuildBody = vxa<Buffer> {
                ftlEngine.render(
                        vertx,
                        "org/jetbrains/research/kotoed/teamcity/requests/triggerBuild.ftl",
                        mapOf("trigger" to triggerBuild),
                        it
                )
            }

            val triggerBuildRes = vxa<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.BuildQueue())
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(triggerBuildBody, it)
            }

            if (HttpResponseStatus.OK.code() == triggerBuildRes.statusCode()) {
                triggerBuildRes.bodyAsJsonObject().getInteger("id")?.let {
                    log.trace("Starting polling for build $it")
                    vertx.deployVerticle(BuildPollerVerticle(it))
                }
            }

            val results = listOf(
                    "triggerBuild" to triggerBuildRes
            ).groupBy { it.second.statusCode() == HttpResponseStatus.OK.code() }

            if (results[false] == null) {
                msg.reply(
                        JsonObject(
                                results[true]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsJsonObject() }
                                        + ("result" to "success")
                        )
                )
            } else {
                msg.reply(
                        JsonObject(
                                results[false]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsString() }
                                        + ("result" to "failed")
                        )
                )
            }
        }
    }

    fun consumeTeamCityBuildInfo(msg: Message<JsonObject>) {

        val wc = WebClient.create(vertx)

        val buildInfo = fromJson<BuildInfo>(msg.body())

        val buildLocator = EmptyLocator *
                DimensionLocator.from("id", buildInfo.id) *
                DimensionLocator.from("project:id", buildInfo.projectId) *
                DimensionLocator.from("number", buildInfo.number) *
                DimensionLocator.from("revision", buildInfo.revision)

        val allBuildLocator =
                if (buildInfo.all ?: false) buildLocator.all() else buildLocator

        launch(UnconfinedWithExceptions(msg)) {
            val buildInfoRes = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Builds + allBuildLocator)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .send(it)
            }

            val results = listOf(
                    "buildInfo" to buildInfoRes
            ).groupBy { it.second.statusCode() == HttpResponseStatus.OK.code() }

            if (results[false] == null) {
                msg.reply(
                        JsonObject(
                                results[true]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsJsonObject() }
                                        + ("result" to "success")
                        )
                )
            } else {
                msg.reply(
                        JsonObject(
                                results[false]!!
                                        .toMap()
                                        .mapValues { e -> e.value.bodyAsString() }
                                        + ("result" to "failed")
                        )
                )
            }
        }
    }

}
