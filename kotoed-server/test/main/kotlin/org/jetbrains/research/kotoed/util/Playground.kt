package org.jetbrains.research.kotoed.util

import org.junit.Test
import kotlin.reflect.jvm.reflect

interface SuperHyperSet<in T> {
    operator fun contains(value: T): Boolean
}

fun <T> SuperHyperSet(func: (T) -> Boolean) = object: SuperHyperSet<T> {
    override operator fun contains(value: T) = func(value)
}

object Universe : SuperHyperSet<Any?> {
    override fun contains(value: Any?): Boolean = true
}

open class A
interface B

fun <T> typeOf(body: () -> T) = body.reflect()!!.returnType

class Playground {
    @Test
    fun whatever() {

        val a = SuperHyperSet<A> { it.hashCode() == 1 }
        val b = SuperHyperSet<B> { it.hashCode() == 2 }

        val c = if(true) b else if(false) a else b
        var d = if(false) a else b

        println(typeOf { a })
        println(typeOf { b })
        println(typeOf { c })
        println(typeOf { d })

        //println(c.contains(CC))

    }
}
