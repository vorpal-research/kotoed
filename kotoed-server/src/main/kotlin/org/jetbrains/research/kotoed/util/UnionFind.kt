package org.jetbrains.research.kotoed.util

import kotlinx.warnings.Warnings

class Subset<T>(val value: T) {
    private var parent: Subset<T>? = null
    private var rank: Int = 0

    val head: Subset<T> get() = parent?.head?.also { parent = it } ?: this

    infix fun union(that: Subset<T>): Subset<T> {
        val thisHead = this.head
        val thatHead = that.head

        return when {
            thisHead.rank < thatHead.rank -> thatHead.also { thisHead.parent = it }
            thisHead.rank > thatHead.rank -> thisHead.also { thatHead.parent = it }
            else -> thisHead.also { thatHead.parent = it; it.rank++ }
        }
    }

    // standard equals() and hashCode() is what we want

    override fun toString() = "Subset($value)"
}

class UnionFind<T> private constructor(val mapping: MutableMap<T, Subset<T>>): Set<T> by mapping.keys {
    constructor(): this(mutableMapOf())

    fun add(value: T): Subset<T> = mapping.getOrPut(value) { Subset(value) }
    fun find(value: T): Subset<T>? = mapping[value]?.head

    @Suppress(Warnings.NOTHING_TO_INLINE)
    inline operator fun get(value: T)=
            find(value) ?: throw NoSuchElementException("No such value: $value")
    fun join(lhv: T, rhv: T): Subset<T> = get(lhv) union get(rhv)
}

@Suppress(Warnings.NOTHING_TO_INLINE)
operator fun<T> UnionFind<T>.plusAssign(value: T){ add(value) }
