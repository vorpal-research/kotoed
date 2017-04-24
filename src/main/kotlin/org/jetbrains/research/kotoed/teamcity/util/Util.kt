package org.jetbrains.research.kotoed.teamcity.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.ext.web.client.HttpRequest
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.HttpHeaderValuesEx
import org.jetbrains.research.kotoed.util.putHeader

fun <T> HttpRequest<T>.putAuthTCHeaders() =
        this.putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)

fun <T> HttpRequest<T>.putDefaultTCHeaders() =
        this.putAuthTCHeaders()
                .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)

fun <T> HttpRequest<T>.isXml() =
        this.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.APPLICATION_XML)

fun name2id(name: String) = "ID[$name]"

fun name2vcs(name: String) = "VCS[$name]"

fun name2build(name: String) = "BUILD[$name]"
