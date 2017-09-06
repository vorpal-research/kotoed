package org.jetbrains.research.kotoed.util

interface WhenChecker {
    operator fun contains(value: Any?): Boolean
}

data class Guard(val predicate:  (Any?) -> Boolean): WhenChecker {
    override operator fun contains(value: Any?) = predicate(value)
}

inline fun<reified T> IsInstance() = Guard { it is T }

data class Equals(val rhv: Any?): WhenChecker {
    override operator fun contains(value: Any?) = value == rhv
}

data class AllOf(val values: List<WhenChecker>): WhenChecker {
    constructor(vararg values: WhenChecker): this(values.asList())

    override fun contains(value: Any?) = values.all { it.contains(value) }
}

infix fun WhenChecker.and(rhv: WhenChecker) = AllOf(this, rhv)
