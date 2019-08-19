package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.buildTemplateByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.BuildSystem.Summary)
@Templatize("buildSystemSummary.jade")
@LoginRequired
@AuthorityRequired(Authority.Teacher)
@JsBundle("buildSystemSummary")
suspend fun handleBuildSystemSummary(context: RoutingContext) {
    context.put(Context.BreadCrumb, BuildSystemBreadCrumb())
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.Title, "Currently running builds")
}

@HandlerFor(UrlPattern.BuildSystem.Status)
@Templatize("buildSystemStatus.jade")
@LoginRequired
@AuthorityRequired(Authority.Teacher)
@JsBundle("buildSystemStatus")
suspend fun handleBuildSystemStatus(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()
    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    context.put(Context.BreadCrumb, BuildSystemStatusBreadCrumb(intId))
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.Title, "Build #$intId")
}
