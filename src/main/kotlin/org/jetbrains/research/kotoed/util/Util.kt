package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
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

fun <T> UnconfinedWithExceptions(msg: Message<T>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key), CoroutineExceptionHandler {
            override fun handleException(context: CoroutineContext, ex: Throwable) {
                ex.printStackTrace()
                msg.reply(
                        JsonObjectOf(
                                "error" to ex.message
                        )
                )
            }
        } + Unconfined
