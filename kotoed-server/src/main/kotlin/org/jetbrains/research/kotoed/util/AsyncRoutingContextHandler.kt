package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch

abstract class AsyncRoutingContextHandler : Handler<RoutingContext>, Loggable {
    protected abstract suspend fun doHandleAsync(context: RoutingContext)
    override fun handle(context: RoutingContext) {
        val coroName = newRequestUUID().let(::CoroutineName)
        log.trace("Assigning ${coroName.name} to $this.doHandleAsync()")
        launch(WithExceptions(context) + VertxContext(context.vertx()) + coroName) {
            doHandleAsync(context)
        }
    }
}