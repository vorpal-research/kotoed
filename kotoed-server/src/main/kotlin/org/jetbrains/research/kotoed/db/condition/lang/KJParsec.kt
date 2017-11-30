package org.jetbrains.research.kotoed.db.condition.lang

import kotlinx.Warnings.DEPRECATION
import org.jparsec.Parsers
import org.jparsec.Scanners
import kotlinx.Warnings.NOTHING_TO_INLINE

@Suppress(NOTHING_TO_INLINE)
data class Parser<out T>(val impl: org.jparsec.Parser<@kotlin.UnsafeVariance T>) {
    inline fun parse(source: CharSequence, moduleName: String?): T = impl.parse(source, moduleName)
    inline fun parse(source: CharSequence): T = impl.parse(source)
    inline fun parse(readable: Readable): T = impl.parse(readable)
    inline fun parse(readable: Readable, moduleName: String?): T = impl.parse(readable, moduleName)
}

@Suppress(NOTHING_TO_INLINE)
class OperatorTable<T>(
        @PublishedApi
        internal val impl: org.jparsec.OperatorTable<@kotlin.UnsafeVariance T> = org.jparsec.OperatorTable()
) {
    inline fun prefix(parser: Parser<(T) -> T>, precedence: Int = 3): OperatorTable<T> =
            impl.prefix(parser.impl.map { java.util.function.Function(it) }, precedence).let(::OperatorTable)

    inline fun postfix(parser: Parser<(T) -> T>, precedence: Int = 3): OperatorTable<T> =
            impl.postfix(parser.impl.map { java.util.function.Function(it) }, precedence).let(::OperatorTable)

    inline fun infixl(parser: Parser<(T, T) -> T>, precedence: Int = 3): OperatorTable<T> =
            impl.infixl(parser.impl.map { java.util.function.BinaryOperator(it) }, precedence).let(::OperatorTable)

    inline fun infixr(parser: Parser<(T, T) -> T>, precedence: Int = 3): OperatorTable<T> =
            impl.infixr(parser.impl.map { java.util.function.BinaryOperator(it) }, precedence).let(::OperatorTable)

    inline fun infixn(parser: Parser<(T, T) -> T>, precedence: Int = 3): OperatorTable<T> =
            impl.infixn(parser.impl.map { java.util.function.BinaryOperator(it) }, precedence).let(::OperatorTable)

    inline fun build(operand: Parser<T>) = impl.build(operand.impl).let(::Parser)
}

fun <T> unaryOperator(symbol: Parser<Any>, function: (T) -> T) = symbol.map { function }
fun <T> binaryOperator(symbol: Parser<Any>, function: (T, T) -> T) = symbol.map { function }

fun <T> operators(body: OperatorTable<T>.() -> Unit) = OperatorTable<T>().let{ it.body(); it }

fun <T, U> Parser<T>.map(f: (T) -> U): Parser<U> = Parser(impl.map(f))
fun <T, U> Parser<T>.followedBy(rhv: Parser<U>) = Parser(impl.followedBy(rhv.impl))
fun <T, U> Parser<T>.precededBy(rhv: Parser<U>) = Parser(Parsers.sequence(rhv.impl, impl) { _, rhv_ -> rhv_ })
fun <T, U> Parser<T>.zip(rhv: Parser<U>) = Parser(Parsers.sequence(this.impl, rhv.impl) { lhv, rhv_ -> Pair(lhv, rhv_) })
fun <T, U, R> Parser<T>.zip(rhv: Parser<U>, f: (T, U) -> R) = Parser(Parsers.sequence(this.impl, rhv.impl, f))

@JvmName("unitFollowedByUnit")
operator fun Parser<Unit>.plus(rhv: Parser<Unit>): Parser<Unit> = followedBy(rhv)

@JvmName("followedByUnit")
operator fun <T> Parser<T>.plus(rhv: Parser<Unit>): Parser<T> = followedBy(rhv)

@JvmName("followedByNonUnit")
operator fun <T, U> Parser<T>.plus(rhv: Parser<U>): Parser<Pair<T, U>> = zip(rhv)

@JvmName("precededByUnit")
operator fun <T> Parser<Unit>.plus(rhv: Parser<T>): Parser<T> = rhv.precededBy(this)

@JvmName("listPlusList")
operator fun <T> Parser<List<T>>.plus(rhv: Parser<List<T>>): Parser<List<T>> =
        zip(rhv) { lhv, rhv_ -> lhv + rhv_ }

@JvmName("elemPlusList")
operator fun <T> Parser<T>.plus(rhv: Parser<List<T>>): Parser<List<T>> =
        zip(rhv) { lhv, rhv_ -> listOf(lhv) + rhv_ }

@JvmName("listPlusElem")
operator fun <T> Parser<List<T>>.plus(rhv: Parser<T>): Parser<List<T>> =
        zip(rhv) { lhv, rhv_ -> lhv + rhv_ }

fun <T> Parser<T>.ignore(): Parser<Unit> = this.map { Unit }
operator fun <T> Parser<T>.unaryMinus() = ignore()

infix fun <T> Parser<T>.or(rhv: Parser<T>): Parser<T>
        = Parser(Parsers.or(impl, rhv.impl))

fun <T> Parser<T>.orNull(): Parser<T?> = or(success(null))

fun <T> recursive(f: (Parser<T>) -> Parser<T>): Parser<T> {
    val ref = org.jparsec.Parser.newReference<T>()
    val lazy = ref.lazy().let(::Parser)
    val parser = f(lazy)
    ref.set(parser.impl)
    return parser
}

fun constant(s: String) = Parser(Scanners.string(s)).map { s }
fun integer() = Parser(Scanners.INTEGER).map { it.toInt() }
fun doubleQuotedString() = Parser(Scanners.DOUBLE_QUOTE_STRING).map { it.substring(1, it.length - 1) }
fun empty(): Parser<Unit> = Parser(Parsers.always())
fun <T> success(value: T) = empty().map { value }
fun identifier(): Parser<String> = Parser(Scanners.IDENTIFIER)

fun <T> Parser<T>.between(l: Parser<Any>, r: Parser<Any>) = impl.between(l.impl, r.impl).let(::Parser)
fun <T> Parser<T>.many(): Parser<List<T>> = impl.many().let(::Parser)
fun <T> Parser<T>.many1(): Parser<List<T>> = impl.many1().let(::Parser)
fun <T> Parser<T>.joinedBy(delimiter: Parser<Any>) = this + (-delimiter + this).many()

fun spaces() = Parser(Scanners.many { it.isWhitespace() })
fun lexeme(contents: String) = constant(contents).between(spaces(), spaces())
