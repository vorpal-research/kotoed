package org.jetbrains.research.kotoed.util.database

import org.jetbrains.research.kotoed.util.expect
import org.jetbrains.research.kotoed.util.snoc
import org.jetbrains.research.kotoed.util.uncheckedCast
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

fun Context<*>.parens(body: Context<*>.() -> Unit) {
    sql('(')
    body()
    sql(')')
}

// json-postgres

class JsonGetField(val obj: Field<Any>, val key: String)
    : CustomField<Any>("->", PostgresDataTypeEx.JSONB) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        parens {
            visit(obj).sql(" -> ").sql("'$key'")
        }
    }
}

class JsonGetElem(val obj: Field<Any>, val index: Int)
    : CustomField<Any>("->", PostgresDataTypeEx.JSONB) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        parens {
            visit(obj).sql(" -> ").sql("'$index'")
        }
    }
}

fun Field<Any>.jsonGet(key: String): Field<Any> = JsonGetField(this, key)
fun Field<Any>.jsonGet(index: Int): Field<Any> = JsonGetElem(this, index)
operator fun Field<Any>.get(key: String) = jsonGet(key)
operator fun Field<Any>.get(index: Int) = jsonGet(index)

class FunctionCall<T: Any>(val function: String, val klass: KClass<T>, val arguments: List<Field<out Any>>)
        : CustomField<T>(function, DSL.getDataType(klass.java)) {
    constructor(function: String, klass: KClass<T>, vararg arguments: Field<out Any>):
            this(function, klass, arguments.asList())

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(dialect().family() == SQLDialect.POSTGRES)
        parens {
            sql(function).parens {
                val (first, rest) = arguments.asSequence().snoc()
                visit(first)
                rest.forEach { sql(','); visit(it) }
            }
        }
    }
}

inline fun <reified T: Any> FunctionCall(function: String, vararg arguments: Field<out Any>) =
        FunctionCall(function, T::class, *arguments)

class TextDocumentMatch(val document: Field<Any>, val query: Field<Any>)
    : CustomField<Boolean>("@@", DSL.getDataType(Boolean::class.java)) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        parens {
            visit(document).sql(" @@ ").visit(query)
        }
    }
}

class TsQueryOrOperator(val lhv: Field<Any>, val rhv: Field<Any>)
    : CustomField<Any>("||", DSL.getDataType(Any::class.java)) {

    override fun accept(ctx: Context<*>) = with(ctx) {
        expect(ctx.dialect().family() == SQLDialect.POSTGRES)
        parens {
            visit(lhv).sql(" || ").visit(rhv)
        }
    }
}

fun toTSQuery(field: Field<String>): Field<Any> = FunctionCall("to_tsquery", DSL.inline("russian"), field)
fun toPlainTSQuery(field: Field<String>): Field<Any> =
        TsQueryOrOperator(
                FunctionCall("plainto_tsquery", DSL.inline("russian"), field),
                FunctionCall("plainto_tsquery", DSL.inline("simple"), field)
        )

infix fun Field<Any>.documentMatch(query: String): Field<Boolean> =
        TextDocumentMatch(this, DSL.field("to_tsquery('russian', '$query')"))
infix fun Field<Any>.documentMatchPlain(query: String): Field<Boolean> =
        TextDocumentMatch(this, DSL.field("plainto_tsquery('russian', '$query')"))

infix fun Field<Any>.documentMatch(query: Field<Any>): Field<Boolean> =
        TextDocumentMatch(this, query)

infix fun Field<Any>.documentMatchRank(query: String): Field<Double> =
        FunctionCall("ts_rank", this, DSL.field("to_tsquery('russian', '$query')"))
infix fun Field<Any>.documentMatchRankPlain(query: String): Field<Double> =
        FunctionCall("ts_rank", this, DSL.field("plainto_tsquery('russian', '$query')"))
infix fun Field<Any>.documentMatchRank(query: Field<String>): Field<Double> =
        FunctionCall("ts_rank", this, query)

