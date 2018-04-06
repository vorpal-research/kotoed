package org.jetbrains.research.kotoed.util

import org.junit.Test
import kotlin.reflect.KProperty

sealed class Symbolic<T>
data class Var<T>(val name: String): Symbolic<T>()
data class Const<T>(val value: T): Symbolic<T>()
data class Add<T>(val lhv: Symbolic<T>, val rhv: Symbolic<T>): Symbolic<T>()
data class Mult<T>(val lhv: Symbolic<T>, val rhv: Symbolic<T>): Symbolic<T>()
data class Constraint<T>(val lhv: Symbolic<T>, val rhv: Symbolic<T>)

object SymbolicDelegate

class SymbolicContext {

    val constraints = mutableListOf<Constraint<*>>()

    operator fun<T> SymbolicDelegate.setValue(self: Any?, property: KProperty<*>, value: Symbolic<T>) {
        constraints += Constraint(Var(property.name), value)
    }

    operator fun SymbolicDelegate.getValue(self: Any?, property: KProperty<*>): Symbolic<Int> {
        return Var(property.name)
    }

    operator fun<T> Symbolic<T>.plus(that: Symbolic<T>) = Add(this, that)
    operator fun<T> Symbolic<T>.times(that: Symbolic<T>) = Mult(this, that)

    operator fun Int.invoke(witness: () -> Unit) = Const(this)

    val Int.c get() = Const(this)

    val re = Regex(""".*[a-z]+""")
}

class PlaygroundTest {


    @Test
    fun whatever() {
        val sc = SymbolicContext()
        with(sc) {
            var x: Symbolic<Int> by SymbolicDelegate
            var z: Symbolic<Int> by SymbolicDelegate
            var y: Symbolic<Int> by SymbolicDelegate

            y = 0.c
            x = x + y + 2.c + 4.c
            x = y
            z = x * x

            listOf(1,2,3).max()

        }

        println(sc.constraints)

    }

}
