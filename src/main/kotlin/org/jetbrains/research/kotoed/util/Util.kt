package org.jetbrains.research.kotoed.util

import com.hazelcast.util.Base64
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpResponse
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.Unconfined
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.full.isSubclassOf

inline suspend fun vxu(crossinline cb: (Handler<AsyncResult<Void?>>) -> Unit): Void? =
        suspendCoroutine { cont ->
            cb(Handler { res ->
                if (res.succeeded()) cont.resume(res.result())
                else cont.resumeWithException(res.cause())
            })
        }

inline suspend fun <T> vxt(crossinline cb: (Handler<T>) -> Unit): T =
        suspendCoroutine { cont ->
            cb(Handler { res -> cont.resume(res) })
        }

inline suspend fun <T> vxa(crossinline cb: (Handler<AsyncResult<T>>) -> Unit): T =
        suspendCoroutine { cont ->
            cb(Handler { res ->
                if (res.succeeded()) cont.resume(res.result())
                else cont.resumeWithException(res.cause())
            })
        }

inline suspend fun <reified T> Loggable.vxal(crossinline cb: (Handler<AsyncResult<T>>) -> Unit): T {
    val res = vxa(cb)
    if (T::class.isSubclassOf(HttpResponse::class)) {
        log.info((res as HttpResponse<*>).bodyAsString())
    } else {
        log.info(res)
    }
    return res
}

inline fun <T> UnconfinedWithExceptions(msg: Message<T>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, exception: Throwable) {
                exception.printStackTrace()
                msg.reply(
                        JsonObject(
                                "result" to "failed",
                                "error" to exception.message
                        )
                )
            }
        } + Unconfined

inline fun UnconfinedWithExceptions(ctx: RoutingContext) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, exception: Throwable) {
                exception.printStackTrace()
                ctx.jsonResponse().end(
                        JsonObject(
                                "result" to "failed",
                                "error" to exception.message
                        )
                )
            }
        } + Unconfined

inline fun <T> defaultWrapperHandlerWithExceptions(crossinline handler: (Message<T>) -> Unit) = { msg: Message<T> ->
    try {
        handler(msg)
    } catch (ex: Exception) {
        ex.printStackTrace()
        msg.reply(
                JsonObject(
                        "result" to "failed",
                        "error" to ex.message
                )
        )
    }
}

inline fun <T> ((Message<T>) -> Unit).withExceptions() =
        defaultWrapperHandlerWithExceptions(this)

interface Loggable {
    val log
        get() = LoggerFactory.getLogger(javaClass)
}

inline fun base64Encode(v: CharSequence): String = String(Base64.encode(v.toString().toByteArray()))
