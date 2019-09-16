package org.jetbrains.research.kotoed.code.vcs

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder
import kotlinx.coroutines.future.await
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.allLines
import org.jetbrains.research.kotoed.util.futureDone
import org.jetbrains.research.kotoed.util.futureExitCode
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

private val UTF8 = Charset.forName("UTF-8")

data class CommandLine(val args: List<String>,
                       val cout: StringBuilder = StringBuilder(),
                       val cerr: StringBuilder = StringBuilder()) : Loggable {
    constructor(vararg vargs: String) : this(vargs.asList())

    data class Output(val rcode: CompletableFuture<Int>, val cout: StringBuilder, val cerr: StringBuilder) {
        suspend fun complete(): Output {
            val exitCode = rcode.await()
            return Output(CompletableFuture.completedFuture(exitCode), cout, cerr)
        }

        override fun toString(): String {
            return "Output(rcode=$rcode, cout=$cout, cerr=$cerr)"
        }

    }

    fun execute(wd: File = File(System.getProperty("user.dir")),
                env: Map<String, String> = mapOf(),
                input: Sequence<String> = sequenceOf()): Output {

        log.info("Running: " + args.joinToString(" "))

        val exitCode = CompletableFuture<Int>()

        val pb = NuProcessBuilder(args).apply {
            environment() += env
            setCwd(wd.toPath())
            setProcessListener(object : NuAbstractProcessHandler() {
                override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
                    cout.append(UTF8.decode(buffer))
                    buffer.position(buffer.limit())
                }

                override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
                    cerr.append(UTF8.decode(buffer))
                    buffer.position(buffer.limit())
                }

                override fun onExit(statusCode: Int) {
                    exitCode.complete(statusCode)
                }
            })
        }.start()
        pb.wantWrite()

        for (l in input) pb.writeStdin(UTF8.encode(l))
        pb.closeStdin(/* force = */ false)

        return Output(exitCode, cout, cerr)
    }

}
