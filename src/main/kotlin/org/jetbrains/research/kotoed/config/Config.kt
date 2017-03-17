package org.jetbrains.research.kotoed.config

import org.jetbrains.research.kotoed.util.base64Encode

class GlobalConfig : Configuration() {
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
