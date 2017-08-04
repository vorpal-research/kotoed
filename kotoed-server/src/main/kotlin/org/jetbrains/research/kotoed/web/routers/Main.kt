package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.util.routing.Templatize

@HandlerFor("/")
@Templatize("main.jade")
@JsBundle("hello")
fun handleIndex(context: RoutingContext) {
    context.put("who", "Kotoed")
}

@HandlerFor("/secret")
@Templatize("secret.jade")
@LoginRequired
@JsBundle("hello")
fun handleSecret(@Suppress("UNUSED_PARAMETER") context: RoutingContext) {}

