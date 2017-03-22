package org.jetbrains.research.kotoed.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch

fun launch(block: suspend CoroutineScope.() -> Unit) {
    launch(Unconfined, block = block)
}
