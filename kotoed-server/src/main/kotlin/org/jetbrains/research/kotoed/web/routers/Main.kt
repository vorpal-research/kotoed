package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.Templatize
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.Index)
@Templatize("main.jade")
@JsBundle("hello")
fun handleIndex(context: RoutingContext) {
    context.put("who", "Kotoed")
}
