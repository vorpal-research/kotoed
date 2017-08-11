@file:Suppress(kotlinx.Warnings.NOTHING_TO_INLINE)

package org.jetbrains.research.kotoed.util

import com.hazelcast.util.Base64
import io.vertx.core.MultiMap
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import kotlinx.Warnings.NOTHING_TO_INLINE
import kotlinx.Warnings.UNCHECKED_CAST
import kotlinx.Warnings.UNUSED_PARAMETER
import java.io.BufferedReader
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass

/******************************************************************************/

inline fun <reified T : Any> klassOf() = T::class
inline fun<T> tryOrNull(body: () -> T) = try { body() } catch (_: Exception) { null }

/******************************************************************************/

object GlobalLogging {
    val log: Logger
        inline get() = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
}

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(javaClass)
}

interface DelegateLoggable : Loggable {
    val loggingClass: Class<*>

    override val log: Logger
        get() = LoggerFactory.getLogger(loggingClass)
}

fun DelegateLoggable(loggingClass: Class<*>) = object : DelegateLoggable {
    override val loggingClass = loggingClass
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

inline fun Map<String, String>.asMultiMap() =
        MultiMap.caseInsensitiveMultiMap().addAll(this)

@JvmName("asMultiMap4KV")
inline fun <K, V> Map<K, V>.asMultiMap() =
        MultiMap.caseInsensitiveMultiMap().addAll(
                this.mapKeys { toString() }.mapValues { toString() })

/******************************************************************************/

@Suppress(UNCHECKED_CAST, NOTHING_TO_INLINE)
inline fun <D> Any?.uncheckedCast(): D = this as D
@Suppress(UNCHECKED_CAST, NOTHING_TO_INLINE)
inline fun <D> Any?.uncheckedCastOrNull(): D? = when(this){ null -> null; else -> uncheckedCast() }

@Suppress(UNUSED_PARAMETER, NOTHING_TO_INLINE)
inline fun <T> use(value: T) {}
@Suppress(UNUSED_PARAMETER, NOTHING_TO_INLINE)
inline fun use(value0: Any?, value1: Any?, vararg values: Any?) {}


inline fun <T> forceType(v: T) = v

// why stdlib doesn't have this?
fun<T> Sequence<T>?.orEmpty() = this ?: emptySequence()
fun<T> Sequence<T>.reduceOrNull(body: (T, T) -> T) = try { reduce(body) } catch (ex: UnsupportedOperationException) { null }

enum class FixAction{ proceed, stop }
fun<T> immutableFix(initial: T, body: (T) -> Pair<FixAction, T>): T {
    var (action, arg) = FixAction.proceed to initial
    while(action != FixAction.stop) {
        val tr = body(arg)
        action = tr.first
        arg = tr.second
    }
    return arg
}

// XXX: think
data class AsyncCache<K, V>(val cache: MutableMap<K, V> = hashMapOf(), val async: suspend (K) -> V) {
    suspend fun getAsync(key: K) = cache.getOrPut(key) { async(key) }
    suspend operator fun invoke(key: K) = getAsync(key)
}

