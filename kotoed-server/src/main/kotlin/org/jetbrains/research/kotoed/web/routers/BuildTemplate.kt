package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.buildTemplateByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.courseByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.BuildTemplateBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.Context
import org.jetbrains.research.kotoed.web.navigation.CourseBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.kotoedNavBar

@HandlerFor(UrlPattern.BuildTemplate.Edit)
@Templatize("buildTemplate.jade")
@LoginRequired
@AuthorityRequired(Authority.Teacher)
@JsBundle("buildTemplateEdit")
suspend fun handleBuildTemplateEdit(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val bt = context.vertx().eventBus().buildTemplateByIdOrNull(intId) ?: run {
        context.fail(HttpResponseStatus.NOT_FOUND)
        return
    }

    context.put(Context.BreadCrumb, BuildTemplateBreadCrumb(bt))
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.Title, "Build template #${bt.id}")
}