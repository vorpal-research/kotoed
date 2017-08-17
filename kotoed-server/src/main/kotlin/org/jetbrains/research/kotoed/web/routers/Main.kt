package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.*
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Index)
@Templatize("main.jade")
@LoginRequired
@JsBundle("hello", withCss = false)
suspend fun handleIndex(context: RoutingContext) {
    context.put(BreadCrumbContextName, RootBreadCrumb)
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put("who", "Kotoed")
}

@HandlerFor(UrlPattern.NotImplemented)
@Templatize("todo.jade")
@EnableSessions
@JsBundle("hello", withCss = false)
suspend fun handleNotImplemented(context: RoutingContext) {}
