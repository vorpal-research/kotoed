package org.jetbrains.research.kotoed.util

import com.intellij.psi.PsiElement
import java.util.function.Consumer

class TreeVisitor {
    fun visitTree(root: PsiElement, consumers: List<Consumer<VisitResult>>) {
        dfs(root, consumers, 0)
    }

    private fun dfs(root: PsiElement, consumers: List<Consumer<VisitResult>>, leafCount: Int): VisitResult {
        if (root.children.isEmpty()) {
            return VisitResult(computeHash(root).toLong(), leafCount, leafCount, leafCount + 1)
        }
        var levelLeafCount = leafCount
        var leftBound = -1
        var levelHash: Long = computeHash(root).toLong()
        for (child in root.children) {
            val nextLevelResult = dfs(child, consumers, levelLeafCount)
            levelLeafCount = nextLevelResult.leafNum
            if (leftBound == -1) {
                leftBound = nextLevelResult.leftBound
            }
            levelHash += nextLevelResult.levelHash
        }
        val currentResult = VisitResult(levelHash, leftBound, levelLeafCount - 1, levelLeafCount)
        if (currentResult.leftBound != currentResult.rightBound) {
            consumers.forEach { it.accept(currentResult) }
        }
        return currentResult
    }
    private fun computeHash(element: PsiElement): Int {
        return element.javaClass.name.hashCode()
    }
}

data class VisitResult(
    val levelHash: Long,
    val leftBound: Int,
    val rightBound: Int,
    val leafNum: Int
)