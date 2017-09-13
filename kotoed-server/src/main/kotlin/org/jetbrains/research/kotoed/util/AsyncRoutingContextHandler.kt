package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch

abstract class AsyncRoutingContextHandler : Handler<RoutingContext>, Loggable {
    protected abstract suspend fun doHandleAsync(context: RoutingContext)
    override fun handle(context: RoutingContext) {
        launch(WithExceptions(context) + VertxContext(context.vertx())) {
            doHandleAsync(context)
        }
    }
}