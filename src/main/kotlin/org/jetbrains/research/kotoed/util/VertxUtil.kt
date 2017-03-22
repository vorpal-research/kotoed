package org.jetbrains.research.kotoed.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.Shareable
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext
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

suspend fun <T> Vertx.executeBlockingAsync(ordered: Boolean = true, body: () -> T): T =
        vxa {
            this.executeBlocking<T>(
                    Handler { fut ->
                        try {
                            fut.complete(body())
                        } catch (ex: Throwable) {
                            fut.fail(ex)
                        }
                    },
                    ordered,
                    it)
        }

suspend fun Vertx.goToEventLoop(): Void =
        vxt { this.runOnContext(it) }

suspend fun clusteredVertxAsync(opts: VertxOptions = VertxOptions()): Vertx =
        vxa { Vertx.clusteredVertx(opts, it) }

suspend fun Vertx.delayAsync(delay: Long): Long =
        vxt { this.setTimer(delay, it) }

object HttpHeaderValuesEx {
    const val APPLICATION_XML = "application/xml"
    const val HTML = "text/html"
}

data class ShareableHolder<T>(val value: T, val vertx: Vertx) : Shareable

fun <T> Vertx.getSharedLocal(name: String, construct: () -> T): T {
    synchronized(this) {
        val map = sharedData().getLocalMap<String, ShareableHolder<T>>(
                "${construct.reflect()?.returnType?.jvmErasure?.qualifiedName}.shared_map"
        )
        var get = map[name]
        if (get == null) {
            get = ShareableHolder(construct(), this)
            map.putIfAbsent(name, get)
        }
        return get.value
    }
}

suspend fun FileSystem.readFileAsync(path: String): Buffer =
        vxa { readFile(path, it) }

suspend fun FileSystem.deleteRecursiveAsync(path: String, recursive: Boolean = true): Unit =
        vxa<Void> { deleteRecursive(path, recursive, it) }.ignore()

data class VertxTimeoutProcessing(val vertx: Vertx,
                                  val time: Long,
                                  var onTimeoutSusp: suspend () -> Unit = {},
                                  var onSuccessSusp: suspend () -> Unit = {},
                                  var bodySusp: suspend () -> Unit = {}) {
    suspend fun execute() {
        var timedOut = false
        val timerId = vertx.setTimer(time){
            launch(Unconfined) {
                vertx.goToEventLoop()
                timedOut = true
                onTimeoutSusp()
            }
        }

        bodySusp()
        vertx.goToEventLoop()
        vertx.cancelTimer(timerId)
        if(timedOut) return
        else onSuccessSusp()
    }

    fun onTimeout(susp: suspend () -> Unit){ onTimeoutSusp = susp }
    fun onSuccess(susp: suspend () -> Unit){ onSuccessSusp = susp }
    fun run(susp: suspend () -> Unit){ bodySusp = susp }
}

suspend fun Vertx.timedOut(time: Long, builder: VertxTimeoutProcessing.() -> Unit) {
    val vtop = VertxTimeoutProcessing(this, time)
    vtop.builder()
    vtop.execute()
}
