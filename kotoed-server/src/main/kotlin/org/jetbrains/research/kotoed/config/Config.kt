package org.jetbrains.research.kotoed.config

import org.jetbrains.research.kotoed.util.base64Encode
import org.jooq.tools.jdbc.JDBCUtils

class GlobalConfig : Configuration() {
    class DebugConfig : Configuration() {
        class DBConfig : Configuration() {
            val Url by "jdbc:postgresql://localhost/kotoed"
            val User by "kotoed"
            val Password by "kotoed"

            val Dialect get() = JDBCUtils.dialect(Url)

            val DataSourceId by "debug.db"
            val PoolSize: Int by { Runtime.getRuntime().availableProcessors() * 2 }
        }

        val Database by DBConfig()

        class MetricsConfig : Configuration() {
            val Enabled by true
        }

        val Metrics by MetricsConfig()
    }

    val Debug by DebugConfig()

    class BuildbotConfig : Configuration() {
        val Host: String by "localhost"
        val Port: Int by 8010
        val EndpointRoot: String by "/api/v2"

        val User: String by "kotoed"
        val Password: String by "change me"
        val AuthString: String by { "Basic ${base64Encode("$User:$Password")}" }
    }

    val Buildbot by BuildbotConfig()

    class VCSConfig : Configuration() {
        val PendingTimeout: Long by 2000L
        val CloneExpire: Long by 86400000L
        val CloneCapacity: Long by 100L
        val StoragePath: String by "vcs"
        val BuildPath: String by "vcs_build"

        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() * 2 }

        val MaxDiffHunkLines: Int by 5000
        val MaxDiffSize: Int by 1000000

        val DefaultEnvironment: Map<String, String> by {
            mapOf(
                    "GIT_ASKPASS" to "true",
                    "SSH_ASKPASS" to "true"
            )
        }
    }

    val VCS by VCSConfig()

    class BuildSystemConfig : Configuration() {
        val StoragePath: String by "builds"
        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() }
        val MaxProcesses: Int by { Runtime.getRuntime().availableProcessors() * 2 }

        val DefaultEnvironment: Map<String, String> by {
            mapOf(
                    "GIT_ASKPASS" to "true",
                    "SSH_ASKPASS" to "true"
            )
        }
    }
    val BuildSystem by BuildSystemConfig()

    class MailConfig : Configuration() {
        val KotoedAddress: String by "kotoed@jetbrains.com"
        val KotoedSignature: String by "Kotoed, the one and only"
        val ServerHost: String by "kspt.icc.spbstu.ru"

        val UseSSL: Boolean by false
        val UseTLS: Boolean by false

        val ServerPort: Int by { if (UseTLS) 587 else if (UseSSL) 465 else 25 }

        val User: String? by Null
        val Password: String? by Null

        class TimeConfig : Configuration() {
            val Hours: Int by 11
            val Minutes: Int by 0
        }

        val SendTime by TimeConfig()
    }

    class WebNotificationConfig : Configuration() {
        val VapidKeyPublic: String by ""
        val VapidKeyPrivate: String by ""
    }

    class NotificationsConfig : Configuration() {
        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() }

        val Mail by MailConfig()
        val Web by WebNotificationConfig()
    }

    val Notifications by NotificationsConfig()

    class ProcessorsConfig : Configuration() {
        val CacheExpiration: Long by 60L
    }

    val Processors by ProcessorsConfig()

    class SecretsConfig : Configuration() {
        val GenericSecret: String by "REPLACE_ME"
    }
    val Secrets by SecretsConfig()

    class RootConfig : Configuration() {
        val ListenPort: Int by 9000
        val PublicUrl: String by "http://localhost:9000"
    }

    val Root by RootConfig()
}

val Config: GlobalConfig = loadConfiguration(GlobalConfig(),
        fromResource(System.getProperty("kotoed.settingsFile", "defaultSettings.json")))
