package org.jetbrains.research.kotoed.util

import com.hazelcast.util.Base64
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.LoggerFactory
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.Unconfined
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

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

inline fun <T> UnconfinedWithExceptions(msg: Message<T>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, exception: Throwable) {
                exception.printStackTrace()
                msg.reply(
                        JsonObject(
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

fun base64Encode(v: CharSequence): String = String(Base64.encode(v.toString().toByteArray()))
