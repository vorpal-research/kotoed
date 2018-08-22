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

data class Moo(var v: Int)

class Playground {
    @Test
    fun whatever() {
        val moo = Moo(2)
        ++moo.v
    }
}
