package org.jetbrains.research.kotoed.web.routers.codereview

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.util.routing.Templatize

@HandlerFor("/codereview/*")
@Templatize("code.jade")
@LoginRequired
@JsBundle("code")
fun handleCode(context: RoutingContext) {
    // Just rendering template. React will do the rest on the client side
}
