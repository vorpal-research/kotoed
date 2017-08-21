package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsonResponse
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.data.Permissions
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

@HandlerFor(UrlPattern.AuthHelpers.WhoAmI)
@JsonResponse
@LoginRequired
suspend fun handleWhoAmI(context: RoutingContext) {
    context.response().end(context.user().principal())
}

@HandlerFor(UrlPattern.AuthHelpers.RootPerms)
@JsonResponse
@LoginRequired
suspend fun handleRootPerms(context: RoutingContext) {
    val user = context.user()
    val isTeacher = user.isAuthorisedAsync(Authority.Teacher)

    context.response().end(Permissions.Root(
            createCourse = isTeacher
    ))
}


@HandlerFor(UrlPattern.AuthHelpers.SubmissionPerms)
@JsonResponse
@LoginRequired
suspend fun handleSubmissionPerms(context: RoutingContext) {
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

    context.response().end(Permissions.Submission(
                    editOwnComments = submissionIsOpen,
                    editAllComments = isTeacher && submissionIsOpen,
                    changeStateOwnComments = submissionIsOpen,
                    changeStateAllComments = isTeacher && submissionIsOpen,
                    postComment = submissionIsOpen
            ))
}
