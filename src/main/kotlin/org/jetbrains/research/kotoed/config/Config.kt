package org.jetbrains.research.kotoed.config

import com.hazelcast.util.Base64
import org.jetbrains.research.kotoed.util.base64Encode

class GlobalConfig: Configuration() {
    class TeamCityConfig: Configuration() {
        val Host by "localhost"
        val Port by 8111
        val EndpointRoot by "/app/rest"

        val User by "kotoed"
        val Password: String by Uninitialized
        val AuthString by { "Basic ${base64Encode("$User:$Password")}" }
    }
    val TeamCity by TeamCityConfig()

    class RootConfig: Configuration() {
        val Port = 9000
    }
    val Root by RootConfig()
}

val Config : GlobalConfig = loadConfiguration(GlobalConfig(), fromResource("defaultSettings.json"))
