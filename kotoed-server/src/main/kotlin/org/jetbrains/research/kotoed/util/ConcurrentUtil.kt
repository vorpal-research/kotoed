package org.jetbrains.research.kotoed.util

import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private data class ProcessExitCode(val process: Process) : Future<Int> {
    var cancelled = false

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone) return false
        else {
            cancelled = true
            return true
        }
    }

    override fun get(timeout: Long, unit: TimeUnit?): Int {
        if (cancelled) throw CancellationException()
        if (process.waitFor(timeout, unit)) return process.exitValue()
        else throw TimeoutException()
    }

    override fun get() = if (cancelled) throw CancellationException() else process.waitFor()
    override fun isDone() = !process.isAlive
    override fun isCancelled() = cancelled
}

val Process.futureExitCode: Future<Int> get() = ProcessExitCode(this)

private data class FutureDone<T>(val value: T) : Future<T> {
    override fun cancel(mayInterruptIfRunning: Boolean) = false
    override fun get() = value
    override fun get(timeout: Long, unit: TimeUnit?) = value
    override fun isDone() = true
    override fun isCancelled(): Boolean = false
}

fun <T> futureDone(value: T): Future<T> = FutureDone(value)
