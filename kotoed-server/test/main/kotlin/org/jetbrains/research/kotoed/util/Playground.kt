package org.jetbrains.research.kotoed.util

import kotlinx.warnings.Warnings.UNUSED_PARAMETER
import kotlin.reflect.full.declaredMemberProperties

fun main(@Suppress(UNUSED_PARAMETER) args: Array<String>) {
    trigger()
}

fun trigger() {

    val capturedMap = mutableMapOf(1 to 42)

    val someData = listOf(42)

    val body = someData.map { datum ->
        object {
            val bug = capturedMap[datum]
            val doNotRemove = null
        }
    }

    (body as List<Any>).first().also { b ->
        println(b.javaClass.kotlin.declaredMemberProperties)
    }

}
