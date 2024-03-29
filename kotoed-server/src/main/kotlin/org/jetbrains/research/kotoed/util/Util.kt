@file:Suppress(kotlinx.warnings.Warnings.NOTHING_TO_INLINE)

package org.jetbrains.research.kotoed.util

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.hazelcast.util.Base64
import io.vertx.core.MultiMap
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import kotlinx.warnings.Warnings.NOTHING_TO_INLINE
import kotlinx.warnings.Warnings.UNCHECKED_CAST
import kotlinx.warnings.Warnings.UNUSED_PARAMETER
import java.io.BufferedReader
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.net.URI
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/******************************************************************************/

inline fun <reified T : Any> klassOf() = T::class
inline fun<T> tryOrNull(body: () -> T) = try { body() } catch (_: Exception) { null }
inline fun catchAll(body: () -> Unit) = try { body() } catch (_: Throwable) {}
inline fun tryOrDump(body: () -> Unit) = try { body() } catch (t: Throwable) { catchAll { t.printStackTrace() } }

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

val enumValueOfCache: LoadingCache<KClass<*>, Method> = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build(
                object : CacheLoader<KClass<*>, Method>() {
                    override fun load(key: KClass<*>) =
                            key.java.getMethod("valueOf", String::class.java)
                }
        )

fun Enum.Companion.valueOf(value: String, klass: KClass<*>): Any =
        enumValueOfCache[klass].invoke(null, value)

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

fun BufferedReader.allLines() = sequence {
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
                return it.next().code
            }

            override fun close() {
                while (it.hasNext()) {
                    it.next()
                }
            }
        }

/******************************************************************************/

fun <T> Sequence<T>.splitBy(predicate: (T) -> Boolean): Sequence<List<T>> =
        sequence {
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
                this.mapKeys { (k, _) -> k.toString() }.mapValues { (_, v) ->  v.toString() })

inline infix fun <K, V> Map<K, Set<V>>.mergeValues(other: Map<K, Set<V>>): Map<K, Set<V>> {
    val keys = this.keys + other.keys

    val res = mutableMapOf<K, Set<V>>()

    for (key in keys) {
        res[key] = (this[key] ?: emptySet()) + (other[key] ?: emptySet())
    }

    return res
}

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

fun<T> Sequence<T>.snoc(): Pair<T, Sequence<T>> = iterator().let { Pair(it.next(), it.asSequence()) }

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

/******************************************************************************/

fun String.normalizeUri() = "${URI(this).normalize()}"

fun Map<String, String>.makeUriQuery() =
        "?" + this.map { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }.joinToString("&")

fun String.truncateAt(index: Int) =
        if(index < length) take(index - 3) + "..."
        else this

fun String.escapeCommonSymbols() =
        replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")

inline operator fun MatchGroupCollection?.component1() = this?.get(0)
inline operator fun MatchGroupCollection?.component2() = this?.get(1)
inline operator fun MatchGroupCollection?.component3() = this?.get(2)
inline operator fun MatchGroupCollection?.component4() = this?.get(3)
inline operator fun MatchGroupCollection?.component5() = this?.get(4)
inline operator fun MatchGroupCollection?.component6() = this?.get(5)
inline operator fun MatchGroupCollection?.component7() = this?.get(6)
inline operator fun MatchGroupCollection?.component8() = this?.get(7)
inline operator fun MatchGroupCollection?.component9() = this?.get(8)
inline operator fun MatchGroupCollection?.component10() = this?.get(9)

inline operator fun<T> List<T>.component6() = this[5]
inline operator fun<T> List<T>.component7() = this[6]
inline operator fun<T> List<T>.component8() = this[7]
inline operator fun<T> List<T>.component9() = this[8]
inline operator fun<T> List<T>.component10() = this[9]

fun <T: Comparable<T>> Pair<T, T>.sorted() = if(first <= second) this else Pair(second, first)
fun <T: Comparable<T>> Triple<T, T, T>.sorted() =
        when {
            first <= second ->
                    when {
                        second <= third -> this
                        first <= third -> Triple(first, third, second)
                        else -> Triple(third, first, second)
                    }
            first <= third -> Triple(second, first, third)
            second <= third -> Triple(second, third, first)
            else -> Triple(third, second, first)
        }

private class MutableAsImmutableList<E>(private val inner: MutableList<E>): List<E> by inner {
    override fun hashCode() = inner.hashCode()
    override fun equals(other: Any?) = inner.equals(other)
    override fun toString() = inner.toString()
}

fun <T> MutableList<T>.asList(): List<T> = MutableAsImmutableList(this)

inline fun <T, S> List<T>.chunksBy(classifier: (T) -> S): List<Pair<S, List<T>>> {
    if (isEmpty()) return emptyList()

    var lastIndex = 0
    var lastClass: S = classifier(this.first())
    val result = mutableListOf<Pair<S, List<T>>>()

    for ((index, e) in asSequence().withIndex().drop(1)) {
        val cls = classifier(e)
        if (cls != lastClass) {
            result += lastClass to subList(lastIndex, index)
            lastClass = cls
            lastIndex = index
        }
    }

    result += lastClass to subList(lastIndex, size)
    return result.asList()
}

operator fun<T: Comparable<T>> ClosedRange<T>.contains(that: ClosedRange<T>) =
        contains(that.start) && contains(that.endInclusive)

fun String.decapitalize(): String = replaceFirstChar { it.lowercase(Locale.getDefault()) }
fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

class GenericKType<T>(val inner: KType) {
    override fun equals(other: Any?): Boolean = when {
        other is GenericKType<*> -> inner.equals(other.inner)
        else -> inner.equals(other)
    }
    override fun hashCode(): Int = inner.hashCode()
    override fun toString(): String = inner.toString()
}
infix fun <T> GenericKType<List<*>>.of(argument: GenericKType<T>): GenericKType<List<T>> =
    GenericKType(List::class.createType(listOf(KTypeProjection.invariant(argument.inner))))

fun <T: Any> GenericKType(kClass: KClass<out T>) = GenericKType<T>(kClass.starProjectedType)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> genericTypeOf(): GenericKType<T> = GenericKType(typeOf<T>())
