package org.jetbrains.research.kotoed.statistics

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.data.buildbot.build.LogType.STDIO
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.sql.Timestamp
import java.time.Instant

@AutoDeployable
class JUnitStatisticsVerticle : AbstractKotoedVerticle(), Loggable {

    private val template = "TEST-.*\\.xml".toRegex()

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.LogContent)
    suspend fun consumeLogContent(logContent: LogContent) {
        if (template !in logContent.logName()) return

        log.trace("Processing log $logContent")

        val xml = logContent.content.byteInputStream()

        val json = xml2json(xml)

        val build = dbFindAsync(BuildRecord().setBuildRequestId(logContent.buildRequestId()))
                .firstOrNull() ?: throw IllegalStateException(
                "Build request ${logContent.buildRequestId()} not found")

        val result: SubmissionResultRecord = SubmissionResultRecord().apply {
            submissionId = build.submissionId
            time = Timestamp.from(Instant.now())
            type = logContent.logName()
                    .splitToSequence('/')
                    .last()
            body = json
        }

        dbCreateAsync(result)
    }
}

@AutoDeployable
class KFirstRunnerVerticle : AbstractKotoedVerticle(), Loggable {

    private val template = "results\\.json".toRegex()

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.LogContent)
    suspend fun consumeLogContent(logContent: LogContent) {
        if (template !in logContent.logName()) return

        log.trace("Processing log $logContent")

        val json = JsonObject(logContent.content)

        val build = dbFindAsync(BuildRecord().setBuildRequestId(logContent.buildRequestId()))
                .firstOrNull() ?: throw IllegalStateException(
                "Build request ${logContent.buildRequestId()} not found")

        val result: SubmissionResultRecord = SubmissionResultRecord().apply {
            submissionId = build.submissionId
            time = Timestamp.from(Instant.now())
            type = logContent.logName()
                    .splitToSequence('/')
                    .last()
            body = json
        }

        dbCreateAsync(result)
    }
}

@AutoDeployable
class BuildLogVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Buildbot.Build.LogContent)
    suspend fun consumeLogContent(logContent: LogContent) {
        if (0 == logContent.results()) return

        log.trace("Processing log $logContent")

        val content = when (logContent.logType()) {
            STDIO -> {
                logContent.content
                        .lineSequence()
                        .map { it.drop(1) }
                        .joinToString("\n")
            }
            else -> {
                logContent.content
            }
        }

        val json = JsonObject("log" to content)

        val build = dbFindAsync(BuildRecord().setBuildRequestId(logContent.buildRequestId()))
                .firstOrNull() ?: throw IllegalStateException(
                "Build request ${logContent.buildRequestId()} not found")

        val result: SubmissionResultRecord = SubmissionResultRecord().apply {
            submissionId = build.submissionId
            time = Timestamp.from(Instant.now())
            type = "Failed build log for ${logContent.stepName()}/${logContent.logName()}"
            body = json
        }

        dbCreateAsync(result)
    }
}
