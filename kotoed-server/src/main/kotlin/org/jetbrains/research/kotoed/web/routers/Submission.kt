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
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Submission.Index)
@Templatize("submissionDetails.jade")
@LoginRequired
@JsBundle("submissionDetails")
suspend fun handleSubmissionIndex(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val (course, author, project, submission) =
            SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    context.put(BreadCrumbContextName, SubmissionBreadCrumb(course, author, project, submission))
    context.put(NavBarContextName, kotoedNavBar(context.user()))
}