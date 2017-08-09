package org.jetbrains.research.kotoed.statistics

import io.vertx.core.json.JsonObject
import kotlinx.Warnings.UNUSED_PARAMETER
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.PostgresDataTypeEx
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jooq.Field
import org.jooq.impl.DSL.field
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

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.LogContent)
    suspend fun consumeLogContent(logContent: LogContent) {
        if (template !in logContent.logName) return

        log.trace("Processing log $logContent")

        val xmlStream = logContent.content.byteInputStream()

        val artifactBody = xml2json(xmlStream)

        val data = handlers.map { handler ->
            Pair(handler, handler.call(logContent, artifactBody))
        }

        val ds = vertx.getSharedDataSource(
                Config.Debug.Database.DataSourceId,
                Config.Debug.Database.Url,
                Config.Debug.Database.User,
                Config.Debug.Database.Password
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

@Suppress(UNUSED_PARAMETER)
internal object JUnit {
    private val buildIdRegex = "(?<=id:)\\d+".toRegex()

    fun build_id(logContent: LogContent, json: JsonObject): Int {
        return logContent.buildId
    }

    fun artifact_name(logContent: LogContent, json: JsonObject): String {
        return logContent.logName
    }

    fun artifact_body(logContent: LogContent, json: JsonObject): JsonObject {
        return json
    }

    fun total_test_count(logContent: LogContent, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("tests")
                .toInt()
    }

    fun failed_test_count(logContent: LogContent, json: JsonObject): Int {
        return json.getJsonObject("testsuite")
                .getString("failures")
                .toInt()
    }
}
