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
        }

        val DB by DBConfig()

        class MetricsConfig : Configuration() {
            val Enabled by true
        }

        val Metrics by MetricsConfig()
    }

    val Debug by DebugConfig()

    class TeamCityConfig : Configuration() {
        val Host: String by "localhost"
        val Port: Int by 8111
        val EndpointRoot: String by "/app/rest"

        val User: String by "kotoed"
        val Password: String by Uninitialized
        val AuthString: String by { "Basic ${base64Encode("$User:$Password")}" }
    }

    val TeamCity by TeamCityConfig()

    class VCSConfig : Configuration() {
        val PendingTimeout: Long by 2000L
        val CloneExpire: Long by 86400000L
        val CloneCapacity: Long by 100L
        val StoragePath: String by "vcs"

        val PoolSize: Int by { Runtime.getRuntime().availableProcessors() * 2 }
    }

    val VCS by VCSConfig()

    class RootConfig : Configuration() {
        val Port: Int by 9000
    }

    val Root by RootConfig()
}

val Config: GlobalConfig = loadConfiguration(GlobalConfig(),
        fromResource(System.getProperty("kotoed.settingsFile", "defaultSettings.json")))
