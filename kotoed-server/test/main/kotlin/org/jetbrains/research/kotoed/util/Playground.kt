package org.jetbrains.research.kotoed.util

import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.StartedProcess
import org.zeroturnaround.exec.listener.ProcessListener
import java.io.File
import java.util.concurrent.*
import java.util.function.Supplier
import kotlin.coroutines.experimental.suspendCoroutine

class AsyncProcessExecutor(val service: ExecutorService = Executors.newSingleThreadExecutor()) : ProcessExecutor() {
    override fun <T : Any?> invokeSubmit(executor: ExecutorService?, task: Callable<T>): CompletableFuture<T> =
            CompletableFuture.supplyAsync(Supplier { task.call() }, service)

    override fun newExecutor(processName: String?): ExecutorService? = null
}

suspend fun ProcessExecutor.executeAsync(): ProcessResult {
    val future = start().future
    if(future !is CompletableFuture)
        throw IllegalArgumentException("Use BetterProcessExecutor please")

    return future.await()
}

suspend fun peton() =
        AsyncProcessExecutor()
                    .command("python", "-c", "foo(bar)")
                    .directory(File("/home/belyaev"))
                    .readOutput(true)
                    .redirectErrorStream(true)
                    .executeAsync()

class Playground {
    @Test
    fun whatever() {
        runBlocking {
            peton().let { it.output.lines.forEach { println(it) } }
            peton().let { it.output.lines.forEach { println(it) } }
        }
    }
}
