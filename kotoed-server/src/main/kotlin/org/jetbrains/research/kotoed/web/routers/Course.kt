package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.util.routing.Templatize
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.ProjectWithRelated
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import org.jetbrains.research.kotoed.web.eventbus.courseByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Course.Index)
@Templatize("projects.jade")
@LoginRequired
@JsBundle("projectList")
suspend fun handleCourseIndex(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val course = context.vertx().eventBus().courseByIdOrNull(intId) ?: run {
        context.fail(HttpResponseStatus.NOT_FOUND)
        return
    }

    context.put(Context.BreadCrumb, CourseBreadCrumb(course))
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.Title, course.name)
}