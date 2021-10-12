package org.jetbrains.research.kotoed.code

import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.code.getPsi
import org.jetbrains.research.kotoed.util.code.temporaryKotlinEnv
import org.junit.Test
import kotlin.test.assertEquals

class KotlinCompilerTest: Loggable {
    @Test
    fun testKotlinCompile() {

        val code =
            //language=Kotlin
            """
            fun foo(): Int = 34
            fun foo(): Int = 3
            """

        val file = temporaryKotlinEnv {
            getPsi(code)
        }

        val f = file.declarations.find { it is KtFunction }
        assertEquals("foo", f!!.name)
    }
}