package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.HandlerFor
import org.jetbrains.research.kotoed.util.JsBundle
import org.jetbrains.research.kotoed.util.LoginRequired
import org.jetbrains.research.kotoed.util.Templatize

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
fun handleSecret(context: RoutingContext) {}

