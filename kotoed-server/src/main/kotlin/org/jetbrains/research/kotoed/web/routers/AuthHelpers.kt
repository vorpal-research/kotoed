package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsonResponse
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.data.Permissions
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
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

@HandlerFor(UrlPattern.AuthHelpers.CoursePerms)
@JsonResponse
@LoginRequired
suspend fun handleCoursePerms(context: RoutingContext) {
    val user = context.user()
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val courseWrapper: DbRecordWrapper =
            context.vertx().eventBus().sendJsonableAsync(
                    Address.Api.Course.Read,
                    CourseRecord().apply {
                        this.id = intId
                    })

    context.response().end(Permissions.Course(
            createProject = courseWrapper.verificationData.status == VerificationStatus.Processed
    ))
}

@HandlerFor(UrlPattern.AuthHelpers.ProjectPerms)
@JsonResponse
@LoginRequired
suspend fun handleProjectPerms(context: RoutingContext) {
    val user = context.user()
    val id by context.request()
    val intId = id?.toInt()

    val isTeacher = user.isAuthorisedAsync(Authority.Teacher)

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val projectWrapper: DbRecordWrapper =
            context.vertx().eventBus().sendJsonableAsync(
                    Address.Api.Project.Read,
                    ProjectRecord().apply {
                        this.id = intId
                    })

    val project: ProjectRecord = projectWrapper.record.toRecord()

    context.response().end(Permissions.Project(
            createSubmission = projectWrapper.verificationData.status == VerificationStatus.Processed &&
                    user?.principal()?.get("id") == project.denizenId,
            deleteProject = isTeacher
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

    val (_, author, _, _, submission) =
        SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
            context.fail(HttpResponseStatus.NOT_FOUND)
            return
        }


    val submissionIsOpen = submission.state == SubmissionState.open

    context.response().end(Permissions.Submission(
                    editOwnComments = submissionIsOpen,
                    editAllComments = isTeacher && submissionIsOpen,
                    changeStateOwnComments = submissionIsOpen,
                    changeStateAllComments = isTeacher && submissionIsOpen,
                    postComment = submissionIsOpen,
                    resubmit = author.id == context.user()?.principal()?.getInteger("id"),
                    changeState = isTeacher,
                    clean = isTeacher,
                    tags = isTeacher
            ))
}
