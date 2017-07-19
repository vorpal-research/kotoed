package org.jetbrains.research.kotoed.data.buildbot.build

import org.jetbrains.research.kotoed.data.EventBusDatum

data class TriggerBuild(
        val schedulerId: String,
        val revision: String
) : EventBusDatum<TriggerBuild>()

data class BuildRequestInfo(
        val buildRequestId: String
) : EventBusDatum<BuildRequestInfo>()

data class BuildCrawl(
        val buildId: Int
) : EventBusDatum<BuildCrawl>()

data class StepCrawl(
        val buildId: Int,
        val stepId: Int
) : EventBusDatum<StepCrawl>()

data class LogCrawl(
        val buildId: Int,
        val stepId: Int,
        val logId: Int,
        val logName: String
) : EventBusDatum<LogCrawl>()

data class LogContent(
        val buildId: Int,
        val stepId: Int,
        val logId: Int,
        val logName: String,
        val content: String
) : EventBusDatum<LogContent>()
