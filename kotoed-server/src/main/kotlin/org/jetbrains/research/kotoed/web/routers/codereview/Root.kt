package org.jetbrains.research.kotoed.web.routers.codereview

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.isProjectOwnerOrTeacher
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.CodeReview.Index)
@Templatize("code.jade")
@LoginRequired
@JsBundle("code")
suspend fun handleCode(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val (course, author, authorProfile, project, submission) =
            SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    if (!context.user().isProjectOwnerOrTeacher(context.vertx(), project)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.BreadCrumb, SubmissionReviewBreadCrumb(course, author, authorProfile, project, submission))
    context.put(Context.Title, "Submission #${submission.id} Review")
}
