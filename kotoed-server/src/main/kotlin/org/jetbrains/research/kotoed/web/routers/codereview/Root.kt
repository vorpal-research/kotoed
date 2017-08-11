package org.jetbrains.research.kotoed.web.routers.codereview

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.web.CodeReview
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorised
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

@HandlerFor("/codereview/:id/*")
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

    val submission = context.vertx().eventBus().submissionByIdOrNull(intId) ?: run {
        context.fail(HttpResponseStatus.NOT_FOUND)
        return
    }

    if (submission.state == SubmissionState.pending || submission.state == SubmissionState.invalid) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }
}

private typealias Capabilities = CodeReview.Capabilities
private typealias Permissions = CodeReview.Permissions


/**
 * Endpoint that return user capabilities inside code review.
 */
@HandlerFor("/codereview-api/caps/:id") // To avoid clash with code review app itself
@JsonResponse
@LoginRequired
suspend fun handleCapabilities(context: RoutingContext) {
    val user = context.user()
    val isTeacher = user.isAuthorised("teacher")
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
