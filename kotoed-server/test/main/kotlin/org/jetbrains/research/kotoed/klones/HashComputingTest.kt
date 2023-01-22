package org.jetbrains.research.kotoed.klones

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.research.kotoed.db.processors.SubmissionProcessorVerticle
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.code.getPsi
import org.jetbrains.research.kotoed.util.code.temporaryKotlinEnv
import org.junit.Test
import kotlin.test.assertEquals

class HashComputingTest: Loggable {
    val subProcessorVarticle = SubmissionProcessorVerticle()

    @Test
    fun differentExpressionEqualsHashesTest() {
        val firstFunction =
            //language=Kotlin
            """
            fun foo(a:Int, b:Int): Int = a + b
            """
        val secondFunction =
            //language=Kotlin
            """
            fun bar(cap:Int, barbaris:Int): Int = cap / barbaris
            """

        val firstFun = getPsiElementFromString(firstFunction)
        val hashesForFirstFunction = subProcessorVarticle.computeHashesForElement(firstFun)
        val secondFun = getPsiElementFromString(secondFunction)
        val hashesForSecondFunction = subProcessorVarticle.computeHashesForElement(secondFun)
        assertEquals(hashesForFirstFunction, hashesForSecondFunction)
    }

    private fun getPsiElementFromString(expression: String): PsiElement {
        val firstFunctionFile = temporaryKotlinEnv {
            getPsi(expression)
        }
        return firstFunctionFile
    }
}