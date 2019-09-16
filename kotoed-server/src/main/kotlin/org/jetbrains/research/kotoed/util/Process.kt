package org.jetbrains.research.kotoed.util

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder
import com.zaxxer.nuprocess.NuProcessHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AsyncProcess(val callback: (AsyncProcess) -> Unit) : NuAbstractProcessHandler() {
    var exitCallback: ((Int) -> Unit)? = null
    var exitCode: Int? = null
    lateinit var nuProcess: NuProcess

    override fun onPreStart(nuProcess: NuProcess) {
        this.nuProcess = nuProcess
    }

    override fun onExit(statusCode: Int) {
        exitCallback?.invoke(statusCode)
        exitCode = statusCode
    }

    suspend fun wait() = suspendCoroutine<Int> { cont ->
        exitCode?.let { cont.resume(it) }
        exitCallback = { cont.resume(it) }
    }
}

suspend fun NuProcessBuilder.startAsync(): AsyncProcess = suspendCoroutine { cont ->
    val res = AsyncProcess {
        cont.resume(it)
    }
    setProcessListener(res)
    try {
        start()
    } catch (ex: Exception) {
        cont.resumeWithException(ex)
    }
}