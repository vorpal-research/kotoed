package org.jetbrains.research.kotoed.web.routers.codereview

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.web.CodeReview
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorised
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.eventbus.submissionById

@HandlerFor("/codereview/*")
@Templatize("code.jade")
@LoginRequired
@JsBundle("code")
fun handleCode(@Suppress("UNUSED_PARAMETER") context: RoutingContext) {
    // Just rendering template. React will do the rest on the client side
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
        context.response().end(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val submission = context.vertx().eventBus().submissionById(intId)

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
