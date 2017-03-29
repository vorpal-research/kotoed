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
import org.jetbrains.research.kotoed.util.database.PostgresDataTypeEx
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jooq.Field
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.io.ByteArrayInputStream
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

class JUnitStatisticsVerticle : AbstractVerticle(), Loggable {

    private val template = "TEST-.*\\.xml".toRegex()

    private val handlers = listOf<KFunction<Any>>(
            JUnit::buildId,
            JUnit::artifactName,
            JUnit::artifactBody,
            JUnit::totalTestCount,
            JUnit::failedTestCount
    )

    fun field(handler: KFunction<Any>): Field<Any?> {
        return if (handler.returnType.isSubtypeOf(JsonObject::class.createType())) {
            field(handler.name, PostgresDataTypeEx.JSONB)
        } else {
            field(handler.name)
        }
    }

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

            val artifactBody = xml2json(xmlStream)

            val data = handlers.map { handler ->
                Pair(handler, handler.call(artifactContent.path, artifactBody))
            }

            val ds = vertx.getSharedDataSource(
                    "debug.db",
                    Config.Debug.DB.Url,
                    Config.Debug.DB.User,
                    Config.Debug.DB.Password
            )

            jooq(ds).use {
                it.insertInto(table("JUnitStatistics"))
                        .columns(
                                data.map { field(it.first) }
                        )
                        .values(
                                data.map { it.second }
                        )
                        .executeKAsync()
            }
        }
    }
}

internal object JUnit {
    private val buildIdRegex = "(?<=id:)\\d+".toRegex()

    fun buildId(path: String, json: JsonObject): Int {
        return buildIdRegex.find(path)?.run { value.toInt() } ?: -1
    }

    fun artifactName(path: String, json: JsonObject): String {
        return path.split("/").last()
    }

    fun artifactBody(path: String, json: JsonObject): JsonObject {
        return json
    }

    fun totalTestCount(path: String, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("tests")
                .toInt()
    }

    fun failedTestCount(path: String, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("failures")
                .toInt()
    }
}
