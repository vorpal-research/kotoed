@file:Suppress(kotlinx.Warnings.NOTHING_TO_INLINE)

package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import java.lang.Error
import java.lang.reflect.Method
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KFunction

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

            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handleException(handler, exception)
        } + Unconfined

inline fun <T> UnconfinedWithExceptions(msg: Message<T>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handleException(msg, exception)
        } + Unconfined

inline fun UnconfinedWithExceptions(ctx: RoutingContext) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handleException(ctx, exception)
        } + Unconfined

inline fun <T> UnconfinedWithExceptions(har: Handler<AsyncResult<T>>) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = UnconfinedWithExceptions::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    handleException(har, exception)
        } + Unconfined


inline fun UnconfinedWithExceptions(loggable: Loggable) =
        object : AbstractCoroutineContextElement(CoroutineExceptionHandler.Key),
                CoroutineExceptionHandler,
                DelegateLoggable {
            override val loggingClass = loggable::class.java

            override fun handleException(context: CoroutineContext, exception: Throwable) =
                    loggable.log.error("", exception)
        } + Unconfined

// NOTE: suspendCoroutineOrReturn<> is not recommended by kotlin devs, BUT,
// however, suspendCoroutine<>, the only alternative, does *not* work correctly if suspend fun has no
// suspension points.

inline suspend fun <R> KFunction<R>.callAsync(vararg args: Any?) =
        when {
            isSuspend -> suspendCoroutineOrReturn<R> { call(*args, it) }
            else -> throw Error("$this cannot be called as async")
        }

val Method.isKotlinSuspend
    get() = parameters.lastOrNull()?.type == kotlin.coroutines.experimental.Continuation::class.java

inline suspend fun Method.invokeAsync(receiver: Any?, vararg args: Any?) =
        when {
            isKotlinSuspend -> suspendCoroutineOrReturn<Any?> { invoke(receiver, *args, it) }
            else -> throw Error("$this cannot be invoked as async")
        }

/******************************************************************************/
