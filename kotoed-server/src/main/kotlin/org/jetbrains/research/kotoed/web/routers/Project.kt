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
import org.jetbrains.research.kotoed.web.auth.isProjectOwnerOrTeacher
import org.jetbrains.research.kotoed.web.eventbus.ProjectWithRelated
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import org.jetbrains.research.kotoed.web.eventbus.courseByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.projectByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Project.Index)
@Templatize("submissions.jade")
@LoginRequired
@JsBundle("submissionList")
suspend fun handleProjectIndex(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val (course, author, project) =
            ProjectWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    if (!context.user().isProjectOwnerOrTeacher(context.vertx(), project)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(Context.BreadCrumb, ProjectBreadCrumb(course, author, project))
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.Title, "${project.name} by ${author.denizenId}")
}