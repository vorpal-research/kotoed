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
        val Password: String by Uninitialized
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
    }

    val VCS by VCSConfig()

    class MailConfig : Configuration() {
        val KotoedAddress: String by "kotoed@jetbrains.com"
        val KotoedSignature: String by "Kotoed, the one and only"
        val ServerHost: String by "kspt.icc.spbstu.ru"

        val UseSSL: Boolean by false
        val UseTLS: Boolean by false

        val ServerPort: Int by { if (UseTLS) 587 else if (UseSSL) 465 else 25 }

        val User: String? by Null
        val Password: String? by Null
    }

    class NotificationsConfig : Configuration() {
        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() }

        val Mail by MailConfig()
    }

    val Notifications by NotificationsConfig()

    class ProcessorsConfig : Configuration() {
        val CacheExpiration: Long by 30L
    }

    val Processors by ProcessorsConfig()

    class RootConfig : Configuration() {
        val Host: String by "http://localhost"
        val Port: Int by 9000
    }

    val Root by RootConfig()
}

val Config: GlobalConfig = loadConfiguration(GlobalConfig(),
        fromResource(System.getProperty("kotoed.settingsFile", "defaultSettings.json")))
