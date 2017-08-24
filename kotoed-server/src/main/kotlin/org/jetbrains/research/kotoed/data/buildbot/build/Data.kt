package org.jetbrains.research.kotoed.data.buildbot.build

import org.jetbrains.research.kotoed.data.EventBusDatum

data class TriggerBuild(
        val schedulerId: String,
        val revision: String
) : EventBusDatum<TriggerBuild>()

data class BuildRequestInfo(
        val buildRequestId: Int
) : EventBusDatum<BuildRequestInfo>()

data class BuildCrawl(
        val buildRequestId: Int,
        val buildId: Int
) : EventBusDatum<BuildCrawl>()

data class StepCrawl(
        val buildRequestId: Int,
        val buildId: Int,
        val stepId: Int,
        val stepName: String,
        val results: Int?
) : EventBusDatum<StepCrawl>()

enum class LogType(val code: String) {
    TEXT("t"),
    STDIO("s"),
    HTML("h"),
    DELETED("d");

    companion object {
        fun fromCode(code: String): LogType {
            return values().firstOrNull { code == it.code }
                    ?: throw IllegalArgumentException("Unknown log type: $code")
        }
    }
}

data class LogCrawl(
        val buildRequestId: Int,
        val buildId: Int,
        val stepId: Int,
        val stepName: String,
        val results: Int?,
        val logId: Int,
        val logName: String,
        val logType: LogType
) : EventBusDatum<LogCrawl>()

data class LogContent(
        val buildRequestId: Int,
        val buildId: Int,
        val stepId: Int,
        val stepName: String,
        val results: Int?,
        val logId: Int,
        val logName: String,
        val logType: LogType,
        val content: String
) : EventBusDatum<LogContent>()
