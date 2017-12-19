package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.auth.isProjectOwnerOrTeacher
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
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

    val (course, author, authorProfile, project, submission) =
            SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), id_) ?: run {
                context.fail(HttpResponseStatus.NOT_FOUND)
                return
            }

    if (!context.user().isProjectOwnerOrTeacher(project)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.BreadCrumb, SubmissionResultBreadCrumb(course, author, authorProfile, project, submission))
    context.put(Context.Title, "Submission #${submission.id} Results")

}

@HandlerFor(UrlPattern.Comment.Search)
@Templatize("commentSearch.jade")
@AuthorityRequired(Authority.Teacher)
@JsBundle("commentSearch")
suspend fun handleCommentSearch(context: RoutingContext) {
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.BreadCrumb, CommentSearchBreadCrumb)
}

@HandlerFor(UrlPattern.CommentTemplate.Show)
@Templatize("commentTemplateSearch.jade")
@LoginRequired
@JsBundle("commentTemplateSearch")
suspend fun handleCommentTemplates(context: RoutingContext) {
    context.put(Context.NavBar, kotoedNavBar(context.user()))
}