@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/******************************************************************************/

fun launch(block: suspend CoroutineScope.() -> Unit) {
    launch(Unconfined, block = block)
}

/******************************************************************************/

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

/******************************************************************************/

object UnconfinedWithExceptions

inline fun UnconfinedWithExceptions(crossinline handler: (Throwable) -> Unit) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                log.error(exception)
                handler(exception)
            }
        } + Unconfined

inline fun <T> UnconfinedWithExceptions(msg: Message<T>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                log.error(exception)
                msg.fail(
                        0xBEEF,
                        exception.message
                )
            }
        } + Unconfined

inline fun UnconfinedWithExceptions(ctx: RoutingContext) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                log.error(exception)
                ctx.fail(exception)
            }
        } + Unconfined

/******************************************************************************/
