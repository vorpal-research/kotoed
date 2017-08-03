package org.jetbrains.research.kotoed.code.klones

import com.suhininalex.suffixtree.Edge
import com.suhininalex.suffixtree.Node

data class CloneClass(val node: Node) {

    val length = node.parentEdges.sumBy(Edge::length)

    val clones by lazy {
        node.terminalMap.map { e ->
            val (edge, offset) = e
            val to = edge.end - offset
            val from = to - length + 1

            Clone(this, edge[from], edge[to])
        }
    }

}

data class Clone(val parent: CloneClass, val from: Token, val to: Token) {
    val submissionId = from.submissionId
    val file = from.from.filename
    val fromLine = from.from.line
    val toLine = to.to.line
    val location = "$file:$fromLine:$toLine"
    val functionName = from.functionName
}

