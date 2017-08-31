package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.filters.*
import org.jetbrains.research.kotoed.web.eventbus.patchers.PerAddressPatcher

val HarmlessTypes =
        ByTypes(BridgeEventType.RECEIVE,
                BridgeEventType.SOCKET_IDLE,
                BridgeEventType.SOCKET_PING,
                BridgeEventType.SOCKET_CREATED)

val Send = ByType(BridgeEventType.SEND)

fun ProjectOwnerOrTeacher(vertx: Vertx, path: String = "project_id"): BridgeEventFilter =
        ShouldBeProjectOwner(vertx, path) or AuthorityRequired(Authority.Teacher)

fun ProjectOwnerOrTeacherForFilter(vertx: Vertx, path: String = "project_id"): BridgeEventFilter =
        ShouldBeProjectOwnerForFilter(vertx, path) or AuthorityRequired(Authority.Teacher)

fun SubmissionOwnerOrTeacher(vertx: Vertx, path: String = "submission_id"): BridgeEventFilter =
        ShouldBeSubmissionOwner(vertx, path) or AuthorityRequired(Authority.Teacher)

fun SubmissionOwnerOrTeacherForFilter(vertx: Vertx, path: String = "submission_id"): BridgeEventFilter =
        ShouldBeSubmissionOwnerForFilter(vertx, path) or AuthorityRequired(Authority.Teacher)

fun CommentOwnerOrTeacher(vertx: Vertx, path: String = "id"): BridgeEventFilter =
        ShouldBeCommentOwner(vertx, path) or AuthorityRequired(Authority.Teacher)

fun kotoedPerAddressFilter(vertx: Vertx): PerAddress {
    return PerAddress(
            Address.Api.Course.Create to AuthorityRequired(Authority.Teacher),
            Address.Api.Course.Search to Permissive,
            Address.Api.Course.SearchCount to Permissive,

            Address.Api.Notification.Current to Permissive,
            Address.Api.Notification.MarkRead to ShouldBeNotificationTarget(vertx),
            Address.Api.Notification.RenderCurrent to Permissive,

            Address.Api.Project.Create to Permissive,
            Address.Api.Project.Search to AuthorityRequired(Authority.Teacher),
            Address.Api.Project.SearchCount to AuthorityRequired(Authority.Teacher),
            Address.Api.Project.SearchForCourse to Permissive, // filtering is done server-side + patcher covers our asses
            Address.Api.Project.SearchForCourseCount to Permissive, // same as above

            Address.Api.Submission.Code.List to
                    (SubmissionOwnerOrTeacher(vertx) and SubmissionReady(vertx)),
            Address.Api.Submission.Code.Read to
                    (SubmissionOwnerOrTeacher(vertx) and SubmissionReady(vertx)),

            Address.Api.Submission.Comment.Create to
                    (SubmissionOwnerOrTeacher(vertx) and SubmissionOpen(vertx)),
            Address.Api.Submission.Comment.Update to
                    (CommentOwnerOrTeacher(vertx) and CommentSubmissionOpen(vertx)),

            Address.Api.Submission.Comment.Search to AuthorityRequired(Authority.Teacher),
            Address.Api.Submission.Comment.SearchCount to AuthorityRequired(Authority.Teacher),

            Address.Api.Submission.CommentAggregates to
                    (SubmissionOwnerOrTeacher(vertx, "id") and SubmissionReady(vertx, "id")),
            Address.Api.Submission.Comments to
                    (SubmissionOwnerOrTeacher(vertx, "id") and SubmissionReady(vertx, "id")),
            Address.Api.Submission.CommentsTotal to
                    (SubmissionOwnerOrTeacher(vertx, "id") and SubmissionReady(vertx, "id")),

            Address.Api.Submission.Create to ProjectOwnerOrTeacher(vertx),
            Address.Api.Submission.Update to AuthorityRequired(Authority.Teacher),

            Address.Api.Submission.Error to Permissive, // good enough for now
            Address.Api.Submission.History to SubmissionOwnerOrTeacher(vertx),
            Address.Api.Submission.Read to SubmissionOwnerOrTeacher(vertx, "id"),

            Address.Api.Submission.List to ProjectOwnerOrTeacherForFilter(vertx),
            Address.Api.Submission.ListCount to ProjectOwnerOrTeacherForFilter(vertx),

            Address.Api.Submission.Result.Read to SubmissionOwnerOrTeacher(vertx, "id"),

            Address.Api.Submission.Verification.Clean to AuthorityRequired(Authority.Teacher)
    )
}

val KotoedPerAddressAnonymousFilter = PerAddress(
        Address.Api.OAuthProvider.List to Permissive
)

class KotoedFilter(vertx: Vertx) : BridgeEventFilter {
    private val perAddress = kotoedPerAddressFilter(vertx)
    private val perAddressAnonymous = KotoedPerAddressAnonymousFilter
    private val underlying = AnyOf(
            HarmlessTypes,
            Send and perAddressAnonymous,
            LoginRequired and Send and perAddress
    )

    suspend override fun isAllowed(be: BridgeEvent): Boolean = underlying.isAllowed(be)

    fun makePermittedOptions() = perAddress.makePermittedOptions() + perAddressAnonymous.makePermittedOptions()
}

fun kotoedPerAddressPatcher(vertx: Vertx) = PerAddressPatcher(
        Address.Api.Notification.RenderCurrent to NotificationPatcher,
        Address.Api.Project.Create to ProjectCreatePatcher,
        Address.Api.Project.SearchForCourse to CourseListPatcher(vertx),
        Address.Api.Project.SearchForCourseCount to CourseListPatcher(vertx),
        Address.Api.Submission.Comment.Create to CommentCreatePatcher
)

fun kotoedPatcher(vertx: Vertx) = kotoedPerAddressPatcher(vertx)
