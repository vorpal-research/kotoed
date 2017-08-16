package org.jetbrains.research.kotoed.web.routers.codereview

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import kotlinx.html.NAV
import org.jetbrains.research.kotoed.web.data.CodeReview
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.BreadCrumbContextName
import org.jetbrains.research.kotoed.web.navigation.NavBarContextName
import org.jetbrains.research.kotoed.web.navigation.SubmissionReviewBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.kotoedNavBar

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

    val (course, author, project, submission) =
            SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    if (submission.state == SubmissionState.pending || submission.state == SubmissionState.invalid) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, SubmissionReviewBreadCrumb(course, author, project, submission))
}

private typealias Capabilities = CodeReview.Capabilities
private typealias Permissions = CodeReview.Permissions


/**
 * Endpoint that return user capabilities inside code review.
 */
@HandlerFor(UrlPattern.CodeReview.Capabilities) // To avoid clash with code review app itself
@JsonResponse
@LoginRequired
suspend fun handleCapabilities(context: RoutingContext) {
    val user = context.user()
    val isTeacher = user.isAuthorisedAsync(Authority.Teacher)
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val submission = context.vertx().eventBus().submissionByIdOrNull(intId) ?: run {
        context.fail(HttpResponseStatus.NOT_FOUND)
        return
    }

    val submissionIsOpen = submission.state == SubmissionState.open

    context.response().end(CodeReview.Capabilities(
            user.principal(),
            Permissions(
                    editOwnComments = submissionIsOpen,
                    editAllComments = isTeacher && submissionIsOpen,
                    changeStateOwnComments = submissionIsOpen,
                    changeStateAllComments = isTeacher && submissionIsOpen,
                    postComment = submissionIsOpen
            )))
}
