package org.jetbrains.research.kotoed.util

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.junit.Test
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KProperty

// not really tests, but whatever

class Playground {

    object Whatever {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>) = "Whatever"
    }

    @Test
    fun testStuff() {
        val key by Whatever

        launch(Unconfined) {
            val value = suspendCoroutine<Int> { cont -> cont.resume(2) }
            val key2 = key
        }
    }
}
