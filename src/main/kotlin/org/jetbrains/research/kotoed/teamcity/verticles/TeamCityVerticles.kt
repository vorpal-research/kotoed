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
import org.jetbrains.research.kotoed.data.teamcity.build.BuildInfo
import org.jetbrains.research.kotoed.data.teamcity.build.TriggerBuild
import org.jetbrains.research.kotoed.data.teamcity.project.CreateProject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.requests.FreeMarkerTemplateEngineImplEx
import org.jetbrains.research.kotoed.teamcity.util.*
import org.jetbrains.research.kotoed.util.*

// FIXME akhin Split into separate verticles

internal data class Changes(
        val change: List<Change>,
        val href: String,
        val count: Int
) : Jsonable

internal data class Change(
        val id: Int,
        val version: String,
        val username: String,
        val date: String,
        val href: String,
        val webUrl: String
) : Jsonable

@AutoDeployable
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

    suspend fun postToTeamCity(
            template: String,
            templateContext: Map<String, Any?>,
            endpoint: ApiEndpoint
    ): HttpResponse<Buffer> {

        val wc = WebClient.create(vertx)

        val payload = vxa<Buffer> {
            ftlEngine.render(
                    vertx,
                    template,
                    templateContext,
                    it
            )
        }

        val res = vxa<HttpResponse<Buffer>> {
            wc.post(Config.TeamCity.Port, Config.TeamCity.Host, endpoint())
                    .putDefaultTCHeaders()
                    .isXml()
                    .sendBuffer(payload, it)
        }

        return res
    }

    suspend fun processResults(
            msg: Message<JsonObject>,
            results: List<Pair<String, HttpResponse<Buffer>>>
    ) {
        val groups = results.groupBy { it.second.statusCode() == HttpResponseStatus.OK.code() }

        if (groups[false] == null) {
            msg.reply(
                    JsonObject(
                            groups[true]!!
                                    .toMap()
                                    .mapValues { e -> e.value.bodyAsJsonObject() }
                                    + ("result" to "success")
                    )
            )
        } else {
            msg.fail(
                    0xB00B5,
                    groups[false]!!
                            .toMap()
                            .map { e -> "${e.key} -> ${e.value.bodyAsString()}" }
                            .joinToString("\n")
            )
        }
    }

    fun consumeTeamCityProjectCreate(msg: Message<JsonObject>) {
        launch(UnconfinedWithExceptions(msg)) {

            val createProject = fromJson<CreateProject>(msg.body())

            val projectRes = postToTeamCity(
                    "org/jetbrains/research/kotoed/teamcity/requests/createProject.ftl",
                    mapOf("project" to createProject.project),
                    TeamCityApi.Projects
            )

            val vcsRootRes = postToTeamCity(
                    "org/jetbrains/research/kotoed/teamcity/requests/createVcsRoot.ftl",
                    mapOf("vcs" to createProject.vcsRoot),
                    TeamCityApi.VcsRoots
            )

            val buildConfigRes = postToTeamCity(
                    "org/jetbrains/research/kotoed/teamcity/requests/createBuildConfig.ftl",
                    mapOf(
                            "project" to createProject.project,
                            "vcs" to createProject.vcsRoot,
                            "build" to createProject.buildConfig
                    ),
                    TeamCityApi.BuildTypes
            )

            processResults(
                    msg,
                    listOf(
                            "project" to projectRes,
                            "vcsRoot" to vcsRootRes,
                            "buildConfig" to buildConfigRes
                    )
            )
        }
    }

    fun consumeTeamCityBuildTrigger(msg: Message<JsonObject>) {
        launch(UnconfinedWithExceptions(msg)) {

            val wc = WebClient.create(vertx)

            val triggerBuild = fromJson<TriggerBuild>(msg.body())

            val changeLocator = EmptyLocator *
                    DimensionLocator.from("buildType", triggerBuild.buildTypeId) *
                    DimensionLocator.from("version", triggerBuild.revision)

            val changes = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Changes + changeLocator)
                        .putDefaultTCHeaders()
                        .send(it)
            }

            val changeId = if (HttpResponseStatus.OK.code() == changes.statusCode()) {

                fromJson<Change>(changes.bodyAsJsonObject()).id

            } else {
                throw IllegalArgumentException(
                        "Build trigger info invalid\n" +
                                changes.bodyAsString()
                )
            }

            val triggerBuildRes = postToTeamCity(
                    "org/jetbrains/research/kotoed/teamcity/requests/triggerBuild.ftl",
                    mapOf("trigger" to triggerBuild, "changeId" to changeId),
                    TeamCityApi.BuildQueue
            )

            if (HttpResponseStatus.OK.code() == triggerBuildRes.statusCode()) {
                triggerBuildRes.bodyAsJsonObject().getInteger("id")?.let {
                    log.trace("Starting polling for build $it")
                    vertx.deployVerticle(BuildPollerVerticle(it))
                }
            }

            processResults(
                    msg,
                    listOf(
                            "triggerBuild" to triggerBuildRes
                    )
            )
        }
    }

    fun consumeTeamCityBuildInfo(msg: Message<JsonObject>) {
        launch(UnconfinedWithExceptions(msg)) {

            val wc = WebClient.create(vertx)

            val buildInfo = fromJson<BuildInfo>(msg.body())

            val buildLocator = EmptyLocator *
                    DimensionLocator.from("id", buildInfo.id) *
                    DimensionLocator.from("project:id", buildInfo.projectId) *
                    DimensionLocator.from("number", buildInfo.number) *
                    DimensionLocator.from("revision", buildInfo.revision)

            val allBuildLocator =
                    if (buildInfo.all ?: false) buildLocator.all() else buildLocator

            val buildInfoRes = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Builds + allBuildLocator)
                        .putDefaultTCHeaders()
                        .send(it)
            }

            processResults(
                    msg,
                    listOf(
                            "buildInfo" to buildInfoRes
                    )
            )
        }
    }

}
