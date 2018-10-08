package org.jetbrains.research.kotoed.util

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.yield
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.query
import org.jetbrains.research.kotoed.database.Tables
import org.junit.Test
import java.io.File
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

import io.vertx.core.json.JsonObject

fun Process.outLinesChannel(ctx: CoroutineContext): ReceiveChannel<String> = produce(ctx) {
    val reader = inputStream.bufferedReader()
    reader.use {
        while(true) {
            if (reader.ready() || !isAlive) {
                send(reader.readLine() ?: break)
            }
            else {
                yield()
            }
        }
    }
}

fun Process.errLinesChannel(ctx: CoroutineContext): ReceiveChannel<String> = produce(ctx) {
    val reader = errorStream.bufferedReader()
    reader.use {
        while(true) {
            if (reader.ready() || !isAlive) {
                send(reader.readLine() ?: break)
            }
            else {
                yield()
            }
        }
    }
}

fun Process.inLinesChannel(ctx: CoroutineContext): SendChannel<String> = actor(ctx) {
    val writer = outputStream.bufferedWriter()
    writer.use {
        while(true) {
            writer.appendln(receiveOrNull() ?: break)
        }
    }
}

inline suspend fun coroutineContext(): CoroutineContext = suspendCoroutine { c -> c.resume(c.context) }

data class Output(val returnCode: Int, val output: List<String>, val error: List<String>)
class AsyncProcessBuilder(val command: List<String>) {
    constructor(vararg command: String) : this(command.asList())

    val pb = ProcessBuilder(command)
    var outHandler: SendChannel<String>? = null
    var errHandler: SendChannel<String>? = null
    var inputProducer: ReceiveChannel<String>? = null

    fun directory(dir: File) { pb.directory(dir) }

    suspend fun execute(ctx_: CoroutineContext? = null): Int {
        val ctx = ctx_ ?: coroutineContext()
        val process = pb.start()!!
        val out = process.outLinesChannel(ctx)
        val err = process.errLinesChannel(ctx)
        val inp = process.inLinesChannel(ctx)

        run(ctx) {
            while(!out.isClosedForReceive || !err.isClosedForReceive) {
                select<Unit> {
                    if(!err.isClosedForReceive)
                        err.onReceiveOrNull { it?.let{ errHandler?.send(it) } }

                    if(!out.isClosedForReceive)
                        out.onReceiveOrNull { it?.let{ outHandler?.send(it) } }

                    if(inputProducer?.isClosedForReceive == false) {
                        inputProducer?.onReceiveOrNull {
                            when(it) {
                                null -> inp.close()
                                else -> inp.send(it)
                            }
                        }
                    }

                }
            }
        }
        return process.exitValue()
    }

}

class Playground {
    @Test
    fun whatever() {

        val subStates = listOf("open", "closed")
        println(query(Tables.SUBMISSION_RESULT) {
            join(Tables.SUBMISSION) {
                join(Tables.PROJECT) {
                    join(Tables.DENIZEN, field = "denizen_id")
                }
            }
            filter(subStates.map { "submission.state == \"$it\"" }.joinToString(" or "))
        }.toJson().encodePrettily())

        println(fromJson(JsonObject(
                //language=JSON
                """
{
  "filter" : "submission.state == \"open\" or submission.state == \"closed\"",
  "joins" : [ {
    "field" : "submission_id",
    "query" : {
      "joins" : [ {
        "field" : "project_id",
        "query" : {
          "joins" : [ {
            "field" : "denizen_id",
            "query" : {
              "table" : "denizen"
            },
            "result_field" : "denizen"
          } ],
          "table" : "project"
        },
        "result_field" : "project"
      } ],
      "table" : "submission"
    },
    "result_field" : "submission"
  } ],
  "table" : "submission_result"
}
        """), ComplexDatabaseQuery::class).fillDefaults())

    }
}
