package org.jetbrains.research.kotoed.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.Shareable
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

operator fun HttpServerRequest.getValue(thisRef: Nothing?, prop: KProperty<*>): String =
        this.getParam(prop.name)

fun RoutingContext.jsonResponse(): HttpServerResponse =
        this.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)

fun HttpServerResponse.end(json: JsonObject) =
        this.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).end(json.encode())
fun HttpServerResponse.end(json: Jsonable) =
        this.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).end(json.toJson())

suspend fun <T> Vertx.executeBlockingAsync(ordered: Boolean = true, body: () -> T): T  =
        vxa{
            this.executeBlocking<T>(
                    Handler{
                        fut -> try { fut.complete(body()) } catch (ex: Throwable) { fut.fail(ex) }
                    },
                    it)
        }

suspend fun Vertx.goToEventLoop(): Void =
        vxt<Void> { this.runOnContext(it) }

suspend fun clusteredVertxAsync(opts: VertxOptions = VertxOptions()): Vertx =
        vxa { Vertx.clusteredVertx(opts, it) }

suspend fun Vertx.delayAsync(delay: Long): Long =
    vxt { this.setTimer(delay, it) }

object HttpHeaderValuesEx {
    const val APPLICATION_XML = "application/xml"
}

data class ShareableHolder<T>(val value: T, val vertx: Vertx) : Shareable

fun<T> Vertx.getSharedLocal(name: String, construct: () -> T): T {
    synchronized(this) {
        val map = sharedData().getLocalMap<String, ShareableHolder<T>>(
                "${construct.reflect()?.returnType?.jvmErasure?.qualifiedName}.shared_map"
        )
        var get = map[name]
        if(get == null) {
            get = ShareableHolder(construct(), this)
            map.putIfAbsent(name, get)
        }
        return get.value
    }
}
