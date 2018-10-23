package org.jetbrains.research.kotoed.db.condition.lang

import org.apache.commons.lang3.StringEscapeUtils
import org.intellij.lang.annotations.Language

fun String.escape(): String = StringEscapeUtils.escapeJava(this)

private fun String.toQuery() = "\"${this.escape()}\""

private fun Any.toQuery() = "$this".toQuery()

private fun <T> Iterable<T>.toQuery() = map {
    when(it) {
        is Int -> "$it"
        else -> "$it".toQuery()
    }
}.joinToString(",", "[", "]")

private fun Number.toQuery() = "$this"

/**
 * Format string to query.
 *
 * Java String.format syntax is used.
 * NB: You should always use %s for params even if it is a number.
 * The format string is passed to String.format as it is, so you can use features like reordering .
 * Quotes should not be used in your format string.
 *
 * Example: "%s != %s and %s in %s and %s > %s".formatToQuery(str1, str2, str3, list, int1, int2)
 */
fun String.formatToQuery(vararg params: Any?) = this.format(*(params.map {
    when(it) {
        null -> "NULL"
        is String -> it.toQuery()
        is Enum<*> -> it.toString().toQuery()
        is Number -> it.toQuery()
        is Iterable<*> -> it.toQuery()
        else -> it.toQuery()
    }
}.toTypedArray()))