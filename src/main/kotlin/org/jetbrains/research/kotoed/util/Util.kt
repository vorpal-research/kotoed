@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.research.kotoed.util

import com.hazelcast.util.Base64
import io.vertx.core.eventbus.Message
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import java.io.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass

/******************************************************************************/

inline fun <reified T : Any> klassOf() = T::class

/******************************************************************************/

inline fun <T> defaultWrapperHandlerWithExceptions(
        loggable: Loggable?,
        crossinline handler: (Message<T>) -> Unit) = { msg: Message<T> ->
    try {
        handler(msg)
    } catch (ex: Exception) {
        loggable?.apply { log.error(ex) } ?: ex.printStackTrace()
        msg.fail(
                0xC0FFEE,
                ex.message
        )
    }
}

inline fun <T> ((Message<T>) -> Unit).withExceptions(loggable: Loggable? = null) =
        defaultWrapperHandlerWithExceptions(loggable, this)

/******************************************************************************/

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(javaClass)
}

interface DelegateLoggable : Loggable {
    val loggingClass: Class<*>

    override val log: Logger
        get() = LoggerFactory.getLogger(loggingClass)
}

/******************************************************************************/

inline fun base64Encode(v: CharSequence): String =
        String(Base64.encode(v.toString().toByteArray()))

/******************************************************************************/

fun Enum.Companion.valueOf(value: String, klass: KClass<*>) =
        klass.java.getMethod("valueOf", String::class.java).invoke(null, value)

inline fun <reified E : Enum<E>> Enum.Companion.valueOf(value: String) =
        enumValueOf<E>(value)

/******************************************************************************/

fun String.unquote() =
        when {
            startsWith("\"") && endsWith("\"") -> drop(1).dropLast(1)
            startsWith("'") && endsWith("'") -> drop(1).dropLast(1)
            else -> this
        }

/******************************************************************************/

inline fun <T> T?.ignore(): Unit = Unit

/******************************************************************************/

fun BufferedReader.allLines() = buildSequence {
    var line = this@allLines.readLine()
    while (line != null) {
        yield(line)
        line = this@allLines.readLine()
    }
    this@allLines.close()
}

fun Sequence<String>.linesAsCharSequence(): Sequence<Char> =
        flatMap { "$it\n".asSequence() }

fun Sequence<Char>.asInputStream(): InputStream =
        object : InputStream() {
            val it = iterator()

            override fun read(): Int {
                if (!it.hasNext()) return -1
                return it.next().toInt()
            }

            override fun close() {
                while (it.hasNext()) {
                    it.next()
                }
            }
        }

/******************************************************************************/

fun <T> Sequence<T>.splitBy(predicate: (T) -> Boolean): Sequence<List<T>> =
        buildSequence {
            var mut = mutableListOf<T>()
            for (e in this@splitBy) {
                if (predicate(e)) {
                    yield(mut)
                    mut = mutableListOf()
                }
                mut.add(e)
            }
            yield(mut)
        }


/******************************************************************************/

@Suppress("UNCHECKED_CAST")
inline fun <D> Any?.cast(): D = this as D

fun usingWriter(body: (Writer) -> Unit) = StringWriter().apply(body).toString()
fun usingPrintWriter(body: (PrintWriter) -> Unit) = StringWriter().apply { body(PrintWriter(this)) }.toString()
