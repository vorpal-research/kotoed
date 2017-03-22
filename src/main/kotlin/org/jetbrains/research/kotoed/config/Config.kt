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

    class RootConfig : Configuration() {
        val Port: Int by 9000
    }

    val Root by RootConfig()
}

val Config: GlobalConfig = loadConfiguration(GlobalConfig(), fromResource("defaultSettings.json"))
