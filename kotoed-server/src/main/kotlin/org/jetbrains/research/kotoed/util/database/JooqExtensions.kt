package org.jetbrains.research.kotoed.util.database

import org.jetbrains.research.kotoed.util.expect
import org.jooq.*
import org.jooq.impl.CustomField
import org.jooq.impl.DSL
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
inline infix fun <T> Field<T>.equal(other: Field<T>): Condition = this.equal(other)

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Field<T>.equal(other: T): Condition = this.equal(other)

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Field<T>.ne(other: Field<T>): Condition = this.ne(other)

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> Field<T>.ne(other: T): Condition = this.ne(other)


@Suppress("NOTHING_TO_INLINE")
inline fun <E : Any> ResultQuery<*>.fetchInto(klass: KClass<E>): List<E> =
        fetchInto(klass.java)

inline fun <reified E : Any> ResultQuery<*>.fetchInto(): List<E> = fetchInto(E::class.java)
@Suppress("NOTHING_TO_INLINE")
inline fun <E : Any> Result<*>.into(klass: KClass<E>): List<E> =
        into(klass.java)

inline fun <reified E : Any> Result<*>.into(): List<E> = into(E::class.java)
inline fun <reified E : Any> Record.into(): E = into(E::class.java)

// json-postgres

class JsonGetField(val obj: Field<Any>, val key: String)
    : CustomField<Any>("->", PostgresDataTypeEx.JSONB) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        sql('(')
        visit(obj)
        sql(' ')
        sql("->")
        sql(' ')
        sql("'$key'")
        sql(')')
        Unit
    }
}

class JsonGetElem(val obj: Field<Any>, val index: Int)
    : CustomField<Any>("->", PostgresDataTypeEx.JSONB) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        sql('(')
        visit(obj)
        sql(' ')
        sql("->")
        sql(' ')
        sql("$index")
        sql(')')
        Unit
    }
}

fun Field<Any>.jsonGet(key: String): Field<Any> = JsonGetField(this, key)
fun Field<Any>.jsonGet(index: Int): Field<Any> = JsonGetElem(this, index)
operator fun Field<Any>.get(key: String) = jsonGet(key)
operator fun Field<Any>.get(index: Int) = jsonGet(index)
