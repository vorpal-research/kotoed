package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import kotlin.coroutines.experimental.suspendCoroutine

@JvmName("vxVoid")
inline suspend fun vx(crossinline cb: (Handler<AsyncResult<Void?>>) -> Unit): Unit = run {
    suspendCoroutine<Void?> { cont ->
        cb(Handler { res ->
            if (res.succeeded()) cont.resume(res.result())
            else cont.resumeWithException(res.cause())
        })
    }
}

inline suspend fun <T> vx(crossinline cb: (Handler<AsyncResult<T>>) -> Unit): T = run {
    suspendCoroutine<T> { cont ->
        cb(Handler { res ->
            if (res.succeeded()) cont.resume(res.result())
            else cont.resumeWithException(res.cause())
        })
    }
}

interface Loggable {
    val log get() = LoggerFactory.getLogger(javaClass)
}
