package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch

abstract class AsyncRoutingContextHandler : Handler<RoutingContext> {
    protected abstract suspend fun doHandleAsync(context: RoutingContext)
    override fun handle(context: RoutingContext) {
        launch(UnconfinedWithExceptions(context)) {
            doHandleAsync(context)
        }
    }
}