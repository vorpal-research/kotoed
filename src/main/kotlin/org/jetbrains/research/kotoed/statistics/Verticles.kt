package org.jetbrains.research.kotoed.statistics

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.build.ArtifactContent
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.putAuthTCHeaders
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.PostgresDataTypeEx
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jooq.Field
import org.jooq.impl.DSL.field
import java.io.ByteArrayInputStream
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

@AutoDeployable
class JUnitStatisticsVerticle : AbstractKotoedVerticle(), Loggable {

    private val template = "TEST-.*\\.xml".toRegex()

    private val handlers = listOf<KFunction<Any>>(
            JUnit::build_id,
            JUnit::artifact_name,
            JUnit::artifact_body,
            JUnit::total_test_count,
            JUnit::failed_test_count
    )

    fun field(handler: KFunction<Any>): Field<Any?> {
        return if (handler.returnType.isSubtypeOf(JsonObject::class.createType())) {
            field(handler.name, PostgresDataTypeEx.JSONB)
        } else {
            field(handler.name)
        }
    }

    @EventBusConsumerFor(Address.TeamCity.Build.Artifact)
    suspend fun consumeTeamCityArtifact(msg: Message<JsonObject>) {
        val wc = WebClient.create(vertx)

        val artifactContent = fromJson<ArtifactContent>(msg.body())

        if (template !in artifactContent.path) return

        log.trace("Processing artifact ${artifactContent.path}")

        val artifactContentRes = vxa<HttpResponse<Buffer>> {
            wc.get(Config.TeamCity.Port, Config.TeamCity.Host, artifactContent.path)
                    .putAuthTCHeaders()
                    .send(it)
        }

        val xmlStream = ByteArrayInputStream(artifactContentRes.body().bytes)

        val artifactBody = xml2json(xmlStream)

        val data = handlers.map { handler ->
            Pair(handler, handler.call(artifactContent.path, artifactBody))
        }

        val ds = vertx.getSharedDataSource(
                Config.Debug.DB.DataSourceId,
                Config.Debug.DB.Url,
                Config.Debug.DB.User,
                Config.Debug.DB.Password
        )

        jooq(ds).use {
            it.insertInto(Tables.JUNIT_STATISTICS)
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

@Suppress("UNUSED_PARAMETER")
internal object JUnit {
    private val buildIdRegex = "(?<=id:)\\d+".toRegex()

    fun build_id(path: String, json: JsonObject): Int {
        return buildIdRegex.find(path)?.run { value.toInt() } ?: -1
    }

    fun artifact_name(path: String, json: JsonObject): String {
        return path.split("/").last()
    }

    fun artifact_body(path: String, json: JsonObject): JsonObject {
        return json
    }

    fun total_test_count(path: String, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("tests")
                .toInt()
    }

    fun failed_test_count(path: String, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("failures")
                .toInt()
    }
}
