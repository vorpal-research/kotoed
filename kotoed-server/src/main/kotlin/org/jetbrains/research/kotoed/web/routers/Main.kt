package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.*
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Index)
@Templatize("courses.jade")
@LoginRequired
@JsBundle("courseList")
suspend fun handleIndex(context: RoutingContext) {
    context.put(Context.BreadCrumb, RootBreadCrumb)
    context.put(Context.NavBar, kotoedNavBar(context.user()))
}
