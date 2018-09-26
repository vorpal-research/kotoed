package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.SubmissionComments
import org.jetbrains.research.kotoed.data.notification.LinkData
import org.jetbrains.research.kotoed.data.notification.RenderedData
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.commentByIdOrNull

@HandlerFor(UrlPattern.Notification.ById)
@LoginRequired
suspend fun handleNotificationById(context: RoutingContext) {
    val id by context.request()

    id?.toIntOrNull() ?: throw NotFound("Illegal id: $id")

    val eb = context.vertx().eventBus()!!

    val cleanRecord = NotificationRecord().apply { this.id = id.toInt() }

    run<NotificationRecord> {
        eb.sendJsonableAsync(Address.Api.Notification.MarkRead, cleanRecord)
    }

    val rec: RenderedData = eb.sendJsonableAsync(Address.Api.Notification.Render, cleanRecord)

    context.response().redirect(UrlPattern.reverse(
            UrlPattern.Redirect.ById,
            rec.linkTo.toJson().map
    ))
}

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
