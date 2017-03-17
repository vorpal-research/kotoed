package org.jetbrains.research.kotoed.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlin.reflect.KProperty

operator fun HttpServerRequest.getValue(thisRef: Nothing?, prop: KProperty<*>): String =
        this.getParam(prop.name)

fun RoutingContext.jsonResponse(): HttpServerResponse =
        this.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)

fun HttpServerResponse.end(json: JsonObject) = this.end(json.encode())

object HttpHeaderValuesEx {
    const val APPLICATION_XML = "application/xml"
}
