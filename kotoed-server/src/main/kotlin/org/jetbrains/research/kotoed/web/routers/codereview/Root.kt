package org.jetbrains.research.kotoed.web.routers.codereview

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.web.CodeReview
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.isAuthorised
import org.jetbrains.research.kotoed.util.routing.*

@HandlerFor("/codereview/*")
@Templatize("code.jade")
@LoginRequired
@JsBundle("code")
fun handleCode(context: RoutingContext) {
    // Just rendering template. React will do the rest on the client side
}

private typealias Capabilities = CodeReview.Capabilities
private typealias Permissions = CodeReview.Permissions


/**
 * Endpoint that return user capabilities inside code review.
 */
@HandlerFor("/codereview-api/caps") // To avoid clash with code review app itself
@JsonResponse
@LoginRequired
suspend fun handleCapabilities(context: RoutingContext) {
    val user = context.user()
    val isTeacher = user.isAuthorised("teacher")
    context.response().end(CodeReview.Capabilities(
            user.principal(),
            Permissions(
                    editOwnComments = true,
                    editAllComments = isTeacher,
                    changeStateOwnComments = true,
                    changeStateAllComments = isTeacher,
                    postComment = true
            )))
}
