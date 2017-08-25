package org.jetbrains.research.kotoed.db.condition.lang

fun <T> Iterable<T>.inClause() = map {
    when(it) {
        is String -> "\"$it\""
        else -> "$it"
    }
}.joinToString(",", "[", "]")