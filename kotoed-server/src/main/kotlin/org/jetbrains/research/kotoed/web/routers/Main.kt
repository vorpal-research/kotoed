package org.jetbrains.research.kotoed.web.routers

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.Index)
@Templatize("main.jade")
@EnableSessions
@JsBundle("hello")
fun handleIndex(context: RoutingContext) {
    context.put("who", "Kotoed")
}
