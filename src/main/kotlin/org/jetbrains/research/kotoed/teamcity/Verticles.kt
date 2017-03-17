package org.jetbrains.research.kotoed.teamcity

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.project.CreateProject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.requests.FreeMarkerTemplateEngineImplEx
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.util.*

class TeamCityVerticle : AbstractVerticle(), Loggable {

    val ftlEngine = FreeMarkerTemplateEngineImplEx()

    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                Address.TeamCity.Create,
                this@TeamCityVerticle::consumeTeamCityCreate.withExceptions()
        )
    }

    fun consumeTeamCityCreate(msg: Message<JsonObject>) {
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

            vxal<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Projects)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(projectBody, it)
            }

            vxal<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.VcsRoots)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(vcsRootBody, it)
            }

            vxal<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.BuildTypes)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)
                        .sendBuffer(buildConfigBody, it)
            }

            msg.reply(JsonObject("result" to "success"))
        }
    }
}
