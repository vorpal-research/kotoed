package org.jetbrains.research.kotoed.code.klones

import com.suhininalex.suffixtree.Edge
import com.suhininalex.suffixtree.Node
import org.jetbrains.kotlin.js.inline.util.toIdentitySet
import org.jetbrains.research.kotoed.util.dfs

val Node.parentEdges: Sequence<Edge>
    get() {
        val parent = parentEdge ?: return emptySequence()
        return sequenceOf(parent) + parent.parent.parentEdges
    }

val Node.terminalMap: Map<Edge, Int>
    get() {
        val paths = edges.asSequence().flatMap { e ->
            e.dfs {
                val next = terminal ?: return@dfs emptySequence()
                next.edges.asSequence()
            }
        }

        val res = mutableMapOf<Edge, Int>()

        paths.forEach { edge ->
            val offset = res[edge.parent.parentEdge] ?: 0
            res[edge] = offset + edge.length
        }

        val ends = paths.filter { edge -> null == edge.terminal }
                .toIdentitySet()

        res.filterKeys { it in ends }

        return res
    }

val Edge.length: Int
    get() = end - begin + 1

operator fun Edge.get(index: Int): Token = sequence[index] as Token
