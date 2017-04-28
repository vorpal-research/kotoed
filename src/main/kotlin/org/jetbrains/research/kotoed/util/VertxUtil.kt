package org.jetbrains.research.kotoed.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.Shareable
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpRequest
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.slf4j.LoggerFactory
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

/******************************************************************************/

fun RoutingContext.jsonResponse(): HttpServerResponse =
        this.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)

/******************************************************************************/

fun <T> HttpRequest<T>.putHeader(name: CharSequence, value: CharSequence): HttpRequest<T> =
        this.putHeader(name.toString(), value.toString())

operator fun HttpServerRequest.getValue(thisRef: Nothing?, prop: KProperty<*>): String? =
        this.getParam(prop.name)

/******************************************************************************/

suspend fun HttpServerRequest.bodyAsync(): Buffer =
        vxt { bodyHandler(it) }

/******************************************************************************/

fun HttpResponseStatus.toJson(): JsonObject =
        JsonObject("code" to code(), "message" to reasonPhrase())

fun HttpServerResponse.setStatus(status: HttpResponseStatus): HttpServerResponse =
        setStatusCode(status.code()).setStatusMessage(status.reasonPhrase())

fun HttpServerResponse.end(status: HttpResponseStatus): Unit =
        setStatus(status).end(status.toJson())

fun HttpServerResponse.end(json: JsonObject) =
        this.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).end(json.encode())

fun HttpServerResponse.end(json: Jsonable) =
        this.end(json.toJson())

/******************************************************************************/

object HttpHeaderValuesEx {
    const val APPLICATION_XML = "application/xml"
    const val HTML = "text/html"
}

/******************************************************************************/

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
                    it
            )
        }

suspend fun Vertx.goToEventLoop(): Void =
        vxt { this.runOnContext(it) }

suspend fun clusteredVertxAsync(opts: VertxOptions = VertxOptions()): Vertx =
        vxa { Vertx.clusteredVertx(opts, it) }

suspend fun Vertx.delayAsync(delay: Long): Long =
        vxt { this.setTimer(delay, it) }

/******************************************************************************/

data class ShareableHolder<out T>(val value: T, val vertx: Vertx) : Shareable

fun <T> Vertx.getSharedLocal(name: String, construct: () -> T): T {
    synchronized(this) {
        val map = sharedData().getLocalMap<String, ShareableHolder<T>>(
                "${construct.reflect()?.run { returnType.jvmErasure.qualifiedName }}.sharedMap"
        )
        var get = map[name]
        if (get == null) {
            get = ShareableHolder(construct(), this)
            map.putIfAbsent(name, get)
        }
        return get.value
    }
}

/******************************************************************************/

suspend fun FileSystem.readFileAsync(path: String): Buffer =
        vxa { readFile(path, it) }

suspend fun FileSystem.deleteRecursiveAsync(path: String, recursive: Boolean = true): Unit =
        vxu { deleteRecursive(path, recursive, it) }.ignore()

/******************************************************************************/

data class VertxTimeoutProcessing(val vertx: Vertx,
                                  val time: Long,
                                  var onTimeoutSusp: suspend () -> Unit = {},
                                  var onSuccessSusp: suspend () -> Unit = {},
                                  var bodySusp: suspend () -> Unit = {}) {

    suspend fun execute(timeoutCtx: CoroutineContext = Unconfined) {
        var timedOut = false
        val timerId = vertx.setTimer(time) {
            launch(timeoutCtx) {
                vertx.goToEventLoop()
                timedOut = true
                onTimeoutSusp()
            }
        }

        bodySusp()

        vertx.goToEventLoop()
        vertx.cancelTimer(timerId)

        if (timedOut) return
        else onSuccessSusp()
    }

    fun onTimeout(susp: suspend () -> Unit) {
        onTimeoutSusp = susp
    }

    fun onSuccess(susp: suspend () -> Unit) {
        onSuccessSusp = susp
    }

    fun run(susp: suspend () -> Unit) {
        bodySusp = susp
    }
}

suspend fun Vertx.timedOut(time: Long,
                           timeoutCtx: CoroutineContext = Unconfined,
                           builder: VertxTimeoutProcessing.() -> Unit
) {
    val vtop = VertxTimeoutProcessing(this, time)
    vtop.builder()
    vtop.execute(timeoutCtx)
}

/******************************************************************************/

annotation class AutoDeployable

fun autoDeploy(vertx: Vertx, handler: Handler<AsyncResult<CompositeFuture>>) {
    val log = LoggerFactory.getLogger("org.jetbrains.research.kotoed.AutoDeploy")
    val fs = Reflections("org.jetbrains.research.kotoed", TypeAnnotationsScanner(), SubTypesScanner())
            .getTypesAnnotatedWith(AutoDeployable::class.java)
            .map { klass ->
                log.trace("Auto-deploying $klass")

                val f = Future.future<String>()
                vertx.deployVerticle(klass.canonicalName, f)
                f
            }
    CompositeFuture.all(fs).setHandler(handler)
}

annotation class HandlerFor(val path: String, val isRegex: Boolean = false)

fun autoRegisterHandlers(router: Router) {
    val log = LoggerFactory.getLogger("org.jetbrains.research.kotoed.AutoRegisterHandlers")
    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
            .getMethodsAnnotatedWith(HandlerFor::class.java)
            .forEach { method ->
                log.trace("Auto-registering handler $method")

                val anno = method.getAnnotation(HandlerFor::class.java)
                if (anno.isRegex) {
                    router.routeWithRegex(anno.path)
                            .handler { method.invoke(null, it) }
                } else {
                    router.route(anno.path)
                            .handler { method.invoke(null, it) }
                }
            }
}

/******************************************************************************/
