package org.jetbrains.research.kotoed.statistics

import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
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
        if (template !in logContent.logName) return

        log.trace("Processing log $logContent")

        val xml = logContent.content.byteInputStream()

        val json = xml2json(xml)

        val build = dbFindAsync(BuildRecord().setBuildRequestId(logContent.buildId))
                .firstOrNull() ?: throw IllegalStateException(
                "Build ${logContent.buildId} not found")

        val result: SubmissionResultRecord = SubmissionResultRecord().apply {
            submissionId = build.submissionId
            time = Timestamp.from(Instant.now())
            type = logContent.logName
                    .splitToSequence('/')
                    .last()
            body = json
        }

        dbCreateAsync(result)
    }
}
