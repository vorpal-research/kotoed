package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.api.SubmissionComments
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.SubmissionResult
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.redirect
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.auth.isProjectOwnerOrTeacher
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

    if (!context.user().isProjectOwnerOrTeacher(context.vertx(), project)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, SubmissionResultBreadCrumb(course, author, project, submission))

}

@HandlerFor(UrlPattern.Comment.Search)
@Templatize("commentSearch.jade")
@AuthorityRequired(Authority.Teacher)
@JsBundle("commentSearch")
suspend fun handleCommentSearch(context: RoutingContext) {
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, CommentSearchBreadCrumb)
}

@HandlerFor(UrlPattern.Project.Search)
@Templatize("projectSearch.jade")
@AuthorityRequired(Authority.Teacher)
@JsBundle("projectSearch")
suspend fun handleProjectSearch(context: RoutingContext) {
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, ProjectSearchBreadCrumb)
}

@HandlerFor(UrlPattern.Submission.SearchByTags)
@Templatize("submissionByTagsSearch.jade")
@AuthorityRequired(Authority.Teacher)
@JsBundle("submissionByTagsSearch")
suspend fun handleSubmissionByTagsSearch(context: RoutingContext) {
    context.put(NavBarContextName, kotoedNavBar(context.user()))
    context.put(BreadCrumbContextName, SubmissionByTagsSearchBreadCrumb)
}
