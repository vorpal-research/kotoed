package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.api.SubmissionComments
import org.jetbrains.research.kotoed.util.NotFound
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.redirect
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.commentByIdOrNull

@HandlerFor(UrlPattern.Submission.NotificationRedirect)
@LoginRequired
suspend fun handleSubmissionById(context: RoutingContext) {
    val id by context.request()

    id ?: throw NotFound("id is null")

    context.response().redirect(UrlPattern.reverse(
            UrlPattern.Submission.Index,
            mapOf("id" to id)
    ))
}

@HandlerFor(UrlPattern.SubmissionResults.ById)
@LoginRequired
suspend fun handleSubmissionResultsById(context: RoutingContext) {
    val id by context.request()

    id ?: throw NotFound("id is null")

    context.response().redirect(UrlPattern.reverse(
            UrlPattern.Submission.Results,
            mapOf("id" to id)
    ))
}

@HandlerFor(UrlPattern.Comment.ById)
@LoginRequired
suspend fun handleCommentById(context: RoutingContext) {
    val id by context.request()

    val comment = context.vertx().eventBus().commentByIdOrNull(id!!.toInt())
    comment ?: return context.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR)
    if (comment.sourceline != SubmissionComments.UnknownLine && comment.sourcefile != SubmissionComments.UnknownFile)
        context.response().redirect(UrlPattern.reverse(
                UrlPattern.CodeReview.Index,
                mapOf(
                        "id" to comment.submissionId
                ),
                star = "code/${comment.sourcefile}#line=${comment.sourceline}&commentId=${comment.id}"
        ))
    else
        context.response().redirect(UrlPattern.reverse(
                UrlPattern.CodeReview.Index,
                mapOf(
                        "id" to comment.submissionId
                ),
                star = "lost+found#commentId=${comment.id}"
        ))
}
