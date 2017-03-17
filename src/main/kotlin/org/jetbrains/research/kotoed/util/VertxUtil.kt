package org.jetbrains.research.kotoed.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.reflect.KProperty

operator fun HttpServerRequest.getValue(thisRef: Nothing?, prop: KProperty<*>) =
        this.getParam(prop.name)

fun RoutingContext.jsonResponse() =
        this.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)

fun HttpServerResponse.end(json: JsonObject) = this.end(json.encode())

suspend fun <T> Vertx.executeBlockingAsync(ordered: Boolean = true, body: () -> T): T  =
        vxa{
            this.executeBlocking<T>(
                    Handler{
                        fut -> try { fut.complete(body()) } catch (ex: Throwable) { fut.fail(ex) }
                    },
                    it)
        }

suspend fun Vertx.goToEventLoop(): Void =
        vxt { this.runOnContext(it) }

suspend fun clusteredVertxAsync(opts: VertxOptions = VertxOptions()): Vertx =
        vxa { Vertx.clusteredVertx(opts, it) }
