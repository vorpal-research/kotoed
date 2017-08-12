package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.EnableSessions
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.Templatize

@HandlerFor("/login")
@EnableSessions
@Templatize("login.jade")
@JsBundle("login")
fun loginHandler(context: RoutingContext) {

}