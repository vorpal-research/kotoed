package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.auth.User
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.CourseState
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.GlobalLogging.log
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsonResponse
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.data.Permissions
import org.jetbrains.research.kotoed.web.eventbus.SubmissionWithRelated
import kotlin.reflect.typeOf

private val User.denizenId: Int
    get() = principal().getInteger("id")!!

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

    context.response().end(
        Permissions.Root(
            createCourse = isTeacher,
            tags = isTeacher
        )
    )
}

@HandlerFor(UrlPattern.AuthHelpers.CoursePerms)
@JsonResponse
@LoginRequired
suspend fun handleCoursePerms(context: RoutingContext) = withVertx(context.vertx()) {
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val courseWrapper: DbRecordWrapper<CourseRecord> =
        sendJsonableAsync(
            Address.Api.Course.Read,
            CourseRecord().apply {
                this.id = intId
            }
        )

    val projects: List<ProjectRecord> =
        dbFindAsync(ProjectRecord().apply {
            this.courseId = intId
            this.deleted = false
            this.denizenId = context.user().denizenId
        })

    log.debug("Fetched projects: $projects")

    val course: CourseRecord = courseWrapper.record

    val isProcessed = courseWrapper.verificationData.status == VerificationStatus.Processed

    val isOpen = course.state == CourseState.open

    val userIsTeacher = context.user().isAuthorisedAsync(Authority.Teacher)

    context.response().end(
        Permissions.Course(
            createProject = isProcessed && isOpen && (userIsTeacher || projects.isEmpty()),
            editCourse = isProcessed && userIsTeacher,
            viewTags = userIsTeacher
        )
    )
}

@HandlerFor(UrlPattern.AuthHelpers.ProjectPerms)
@JsonResponse
@LoginRequired
suspend fun handleProjectPerms(context: RoutingContext) = withVertx(context.vertx()) {
    val user = context.user()
    val id by context.request()
    val intId = id?.toInt()

    val isTeacher = user.isAuthorisedAsync(Authority.Teacher)

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val projectWrapper: DbRecordWrapper<ProjectRecord> =
        sendJsonableAsync(
            Address.Api.Project.Read,
            ProjectRecord().apply {
                this.id = intId
            })

    val project: ProjectRecord = projectWrapper.record

    val courseWrapper: DbRecordWrapper<CourseRecord> =
        sendJsonableAsync(
            Address.Api.Course.Read,
            CourseRecord().apply {
                this.id = project.courseId
            })

    val course: CourseRecord = courseWrapper.record

    context.response().end(
        Permissions.Project(
            createSubmission = projectWrapper.verificationData.status == VerificationStatus.Processed
                    && user?.denizenId == project.denizenId
                    && course.state == CourseState.open,
            deleteProject = isTeacher
        )
    )
}


@HandlerFor(UrlPattern.AuthHelpers.SubmissionPerms)
@JsonResponse
@LoginRequired
suspend fun handleSubmissionPerms(context: RoutingContext) {
    val user = context.user()
    val id by context.request()
    val intId = id?.toInt()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val (course, author, _, _, submission) =
        SubmissionWithRelated.fetchByIdOrNull(context.vertx().eventBus(), intId) ?: run {
            context.fail(HttpResponseStatus.NOT_FOUND)
            return
        }

    val isTeacher = user.isAuthorisedAsync(Authority.Teacher)
    val isOpen = submission.state == SubmissionState.open
    // && course.state == CourseState.open // otherwise comments do not work
    // for closed courses
    val isResubmittable = (isOpen || submission.state == SubmissionState.invalid)
            && course.state == CourseState.open
    val isAuthor = author.id == context.user()?.denizenId

    context.response().end(
        Permissions.Submission(
            editOwnComments = isOpen,
            editAllComments = isTeacher && isOpen,
            changeStateOwnComments = isOpen,
            changeStateAllComments = isTeacher && isOpen,
            postComment = isOpen,
            resubmit = isAuthor && isResubmittable,
            changeState = isTeacher,
            clean = isTeacher,
            tags = isTeacher,
            klones = isTeacher
        )
    )
}
