package org.jetbrains.research.kotoed.util

import io.netty.util.AsciiString
import io.vertx.ext.web.client.HttpRequest

fun <T> HttpRequest<T>.putHeader(name: AsciiString, value: String) =
        this.putHeader(name.toString(), value)

fun <T> HttpRequest<T>.putHeader(name: AsciiString, value: AsciiString) =
        this.putHeader(name.toString(), value.toString())
