package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.SubmissionResult
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.redirect
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import org.jetbrains.research.kotoed.web.eventbus.commentByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.*

@HandlerFor(UrlPattern.Submission.Results)
@Templatize("submissionResults.jade")
@LoginRequired
@JsBundle("submissionResults")
suspend fun handleSubmissionResults(context: RoutingContext) {
    val id by context.request()
    val id_ = id?.toInt() ?: run {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }
    context.put("submission-id", id_)

    val (course, author, project, submission) =
            SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), id_) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, SubmissionResultBreadCrumb(course, author, project, submission))

}

@HandlerFor(UrlPattern.Comment.ById)
@LoginRequired
suspend fun handleCommentById(context: RoutingContext) {
    val id by context.request()

    val comment = context.vertx().eventBus().commentByIdOrNull(id!!.toInt())
    comment ?: return context.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR)

    context.response().redirect(UrlPattern.reverse(
            UrlPattern.CodeReview.Index,
            mapOf(
                    "id" to comment.submissionId
            ),
            star = "code/${comment.sourcefile}#${comment.sourceline}"
    ))
}

@HandlerFor(UrlPattern.Comment.Search)
@Templatize("commentSearch.jade")
@LoginRequired
@JsBundle("commentSearch")
suspend fun handleCommentSearch(context: RoutingContext) {
    if (!context.user().isAuthorisedAsync(Authority.Teacher)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, CommentSearchBreadCrumb)
}

@HandlerFor(UrlPattern.Project.Search)
@Templatize("projectSearch.jade")
@LoginRequired
@JsBundle("projectSearch")
suspend fun handleProjectSearch(context: RoutingContext) {
    if (!context.user().isAuthorisedAsync(Authority.Teacher)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, ProjectSearchBreadCrumb)
}



