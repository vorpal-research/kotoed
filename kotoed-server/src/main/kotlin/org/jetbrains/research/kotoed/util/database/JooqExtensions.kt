package org.jetbrains.research.kotoed.util.database

import org.jooq.*
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KClass

suspend fun Query.executeKAsync(): Int =
        suspendCoroutine { cont ->
            executeAsync().whenComplete { v, ex ->
                if (ex == null) cont.resume(v)
                else cont.resumeWithException(ex)
            }
        }

suspend fun <T : Record> ResultQuery<T>.fetchKAsync(): Result<T> =
        suspendCoroutine { cont ->
            fetchAsync().whenComplete { v, ex ->
                if (ex == null) cont.resume(v)
                else cont.resumeWithException(ex)
            }
        }

// infix-noninfix overloads are a shadowy corner of Kotlin
// but what not to do in the name of code beauty???
@Suppress("NOTHING_TO_INLINE")
inline infix fun Condition.and(other: Condition): Condition = this.and(other)
@Suppress("NOTHING_TO_INLINE")
inline infix fun Condition.or(other: Condition): Condition = this.or(other)
@Suppress("NOTHING_TO_INLINE")
inline infix fun<T> Field<T>.equal(other: Field<T>): Condition = this.equal(other)
@Suppress("NOTHING_TO_INLINE")
inline infix fun<T> Field<T>.equal(other: T): Condition = this.equal(other)
@Suppress("NOTHING_TO_INLINE")
inline infix fun<T> Field<T>.ne(other: Field<T>): Condition = this.ne(other)
@Suppress("NOTHING_TO_INLINE")
inline infix fun<T> Field<T>.ne(other: T): Condition = this.ne(other)


@Suppress("NOTHING_TO_INLINE")
inline fun<E: Any> ResultQuery<*>.fetchInto(klass: KClass<E>): List<E> =
        fetchInto(klass.java)
inline fun<reified E: Any> ResultQuery<*>.fetchInto(): List<E> = fetchInto(E::class.java)
@Suppress("NOTHING_TO_INLINE")
inline fun<E: Any> Result<*>.into(klass: KClass<E>): List<E> =
        into(klass.java)
inline fun<reified E: Any> Result<*>.into(): List<E> = into(E::class.java)
@Suppress("NOTHING_TO_INLINE")
inline fun<E: Any> Record.into(klass: KClass<E>): E =
        into(klass.java)
inline fun<reified E: Any> Record.into(): E = into(E::class.java)

// json-postgres
