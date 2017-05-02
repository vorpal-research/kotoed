package org.jetbrains.research.kotoed.util

import io.vertx.core.eventbus.ReplyException

object StatusCodes {
    const val FORBIDDEN = 403
    const val INTERNAL_ERROR = 500
    const val NOT_FOUND = 404
    const val TIMED_OUT = 503
    const val BAD_REQUEST = 400
}

data class KotoedException(val code: Int, override val message: String): Exception(message)

data class WrappedException(val inner: Throwable?): Exception(inner)

val Throwable.unwrapped
    get() =
    when (this) {
        is org.jetbrains.research.kotoed.util.WrappedException -> inner ?: this
        else -> this
    }

fun codeFor(ex: Throwable): Int =
        when(ex) {
            is WrappedException -> ex.inner?.let { codeFor(it) } ?: StatusCodes.INTERNAL_ERROR
            is ReplyException -> ex.failureCode()
            is KotoedException -> ex.code
            is IllegalArgumentException, is IllegalStateException ->
                StatusCodes.BAD_REQUEST
            else ->
                StatusCodes.INTERNAL_ERROR
        }
