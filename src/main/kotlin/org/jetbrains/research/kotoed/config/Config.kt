package org.jetbrains.research.kotoed.config

import com.hazelcast.util.Base64

object Config {
    object TeamCity {
        val Host = "localhost"
        val Port = 8111
        val EndpointRoot = "/app/rest"

        val User = "kotoed"
        val Password = "0xDEADCOFFEE"
        val Basic = "Basic ${String(Base64.encode("$User:$Password".toByteArray()))}"
    }

    object Root {
        val Port = 9000
    }
}
