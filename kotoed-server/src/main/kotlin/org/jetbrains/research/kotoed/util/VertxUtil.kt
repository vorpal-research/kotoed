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
import io.vertx.core.shareddata.impl.ClusterSerializable
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
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

val <T> HttpResponse<T>.errorDetails: String
    get() = "${statusMessage()}/${bodyAsString()}"

/******************************************************************************/

fun HttpServerRequest.getRootUrl() =
        URI(absoluteURI()).run {
            "${URI(scheme, userInfo, host, port, "/", null, null)}"
        }

suspend fun <T> HttpRequest<T>.sendAsync() = vxa<HttpResponse<T>> { send(it) }

suspend fun <T> HttpRequest<T>.sendFormAsync(body: MultiMap) = vxa<HttpResponse<T>> { sendForm(body, it) }

suspend fun <T> HttpRequest<T>.sendJsonObjectAsync(obj: JsonObject) = vxa<HttpResponse<T>> { sendJsonObject(obj, it) }

/******************************************************************************/

fun HttpResponseStatus.toJson(): JsonObject =
        JsonObject("code" to code(), "message" to reasonPhrase())

fun HttpServerResponse.setStatus(status: HttpResponseStatus): HttpServerResponse =
        setStatusCode(status.code()).setStatusMessage(status.reasonPhrase())

fun HttpServerResponse.setStatus(ex: Throwable): HttpServerResponse =
        setStatus(HttpResponseStatus.valueOf(codeFor(ex)))

fun HttpServerResponse.end(status: HttpResponseStatus): Unit =
        setStatus(status).end(status.toJson())

fun HttpServerResponse.end(json: JsonObject) =
        putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .end(json.encode())

fun HttpServerResponse.end(json: Jsonable) =
        end(json.toJson())

fun HttpServerResponse.redirect(to: String, status: HttpResponseStatus = HttpResponseStatus.FOUND) =
        putHeader(HttpHeaderNames.LOCATION, to).end(status)

fun RoutingContext.fail(status: HttpResponseStatus) =
        this.fail(status.code())

/******************************************************************************/

object HttpHeaderValuesEx {
    const val APPLICATION_XML = "application/xml"
    const val HTML = "text/html"
    const val HTML_UTF8 = "text/html; charset=utf-8"
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

suspend fun localVertx(opts: VertxOptions = VertxOptions()): Vertx = Vertx.vertx(opts)

suspend fun Vertx.delayAsync(delay: Long): Long =
        vxt { this.setTimer(delay, it) }

/******************************************************************************/

data class ShareableHolder<out T>(val value: T, val vertx: Vertx) : Shareable

@OptIn(ExperimentalReflectionOnLambdas::class)
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

// TODO: Complete rewrite?
data class VertxTimeoutProcessing(val vertx: Vertx,
                                  val time: Long,
                                  private var onTimeoutSusp: suspend () -> Unit = {},
                                  private var onSuccessSusp: suspend () -> Unit = {},
                                  private var bodySusp: suspend () -> Unit = {}) : Loggable {

    suspend fun execute(timeoutCtx: CoroutineContext = vertx.dispatcher() + LogExceptions() + CoroutineName("timeout")) {
        var timedOut = false
        val timerId = vertx.setTimer(time) {
            CoroutineScope(timeoutCtx).launch {
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

suspend fun CoroutineVerticle.timedOut(
        time: Long,
        timeoutCtx: CoroutineContext =
            coroutineContext + DelegateLoggable(VertxTimeoutProcessing::class.java).LogExceptions(),
        builder: VertxTimeoutProcessing.() -> Unit
) {
    val vtop = VertxTimeoutProcessing(vertx, time)
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

                val p = Promise.promise<String>()
                vertx.deployVerticle(klass.canonicalName, p)
                p.future()
            }
    CompositeFuture.all(fs).setHandler(handler)
}

/******************************************************************************/

fun ClusterSerializable.bytes(): ByteArray {
    val buf = Buffer.buffer()
    writeToBuffer(buf)
    return buf.bytes
}

fun <T: ClusterSerializable> T.fromBytes(bytes: ByteArray): T {
    val buf = Buffer.buffer(bytes)
    readFromBuffer(0, buf)
    return this
}
