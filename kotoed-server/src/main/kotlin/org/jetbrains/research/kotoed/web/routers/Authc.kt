package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.*

@HandlerFor("/login")
@Templatize("login.jade")
@JsBundle("hello")
fun login(context: RoutingContext) {}

@HandlerFor("/logout")
@LoginRequired
fun logout(context: RoutingContext) {
    context.session().destroy()
    context.response().setStatus(HttpResponseStatus.FOUND).putHeader("location", "/").end()
}