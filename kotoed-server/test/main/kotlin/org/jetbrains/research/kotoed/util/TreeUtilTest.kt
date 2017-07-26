package org.jetbrains.research.kotoed.util

import org.junit.Test
import kotlin.test.assertEquals

data class Node(val value: Int, val children: List<Node> = listOf()) : Comparable<Node> {
    override fun compareTo(other: Node): Int = value.compareTo(other.value)

    constructor(value: Int, vararg children: Node) : this(value, children.toList())
}

class TreeUtilTest {

    operator fun Int.invoke(vararg value: Node) = Node(this, *value)

    // 1______
    // |      \
    // 3___   2
    // |   \  |\
    // 7   6  5 4__
    // |\  |    |  \
    // 8 9 10   11 12
    val tree =
            1(                            // |
                    3(                    // V
                            7(8(), 9()),  // DFS
                            6(10())
                    ),
                    2(
                            5(),
                            4(11(), 12())
                    )
            )
    //      -> BFS

    val singleton = 1()

    @Test
    fun `test bfs()`() {
        val order = tree.bfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(
                listOf(1, 3, 2, 7, 6, 5, 4, 8, 9, 10, 11, 12),
                order
        )

        val singletonOrder = singleton.bfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(listOf(1), singletonOrder)
    }

    @Test
    fun `test dfs()`() {
        val order = tree.dfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(
                listOf(1, 3, 7, 8, 9, 6, 10, 2, 5, 4, 11, 12),
                order
        )

        val singletonOrder = singleton.dfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(listOf(1), singletonOrder)
    }

    @Test
    fun `test bestfs() without comparator`() {
        val order = tree.bestfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(
                listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                order
        )

        val singletonOrder = singleton.bestfs { children.asSequence() }.map { it.value }.toList()
        assertEquals(listOf(1), singletonOrder)
    }

    @Test
    fun `test bestfs with comparator`() {
        val cmp = naturalOrder<Node>().reversed()
        val order = tree.bestfs(cmp) { children.asSequence() }.map { it.value }.toList()
        assertEquals(
                listOf(1, 3, 7, 9, 8, 6, 10, 2, 5, 4, 12, 11),
                order
        )

        val singletonOrder = singleton.bestfs(cmp) { children.asSequence() }.map { it.value }.toList()
        assertEquals(listOf(1), singletonOrder)
    }
}