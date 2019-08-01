package org.jetbrains.research.kotoed.util

import java.util.*

fun <T> T.dfs(body: T.() -> Sequence<T>): Sequence<T> =
        sequenceOf(this) + body().flatMap{ it.dfs(body) }

inline fun <T> T.rdfs(crossinline body: T.() -> Sequence<T>): Sequence<T> = sequence {
    val stack: Stack<T> = Stack()
    stack += this@rdfs
    while (stack.isNotEmpty()) {
        val e = stack.pop()
        yield(e)
        stack += e.body()
    }
}

inline fun <T> T.bfs(crossinline queConstructor: () -> Queue<T>,
                     crossinline body: T.() -> Sequence<T>) = sequence {
    val que: Queue<T> = queConstructor()
    que += this@bfs
    while (que.isNotEmpty()) {
        val e = que.remove()
        yield(e)
        que += e.body()
    }
}

inline fun <T> T.bfs(crossinline body: T.() -> Sequence<T>) =
        bfs({ java.util.ArrayDeque() }, body)

inline fun <T> T.bestfs(comparator: Comparator<T>, crossinline body: T.() -> Sequence<T>) =
        bfs({ java.util.PriorityQueue(comparator) }, body)

inline fun <T : Comparable<T>> T.bestfs(crossinline body: T.() -> Sequence<T>) =
        bfs({ java.util.PriorityQueue(naturalOrder<T>()) }, body)
