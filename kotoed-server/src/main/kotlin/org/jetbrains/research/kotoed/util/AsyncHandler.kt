package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import kotlinx.coroutines.experimental.launch

abstract class AsyncHandler<E> : Handler<E>, Loggable {
    protected abstract suspend fun handleAsync(event: E)
    override fun handle(event: E) {
        launch(UnconfinedWithExceptions(this)) {
            handleAsync(event)
        }
    }
}