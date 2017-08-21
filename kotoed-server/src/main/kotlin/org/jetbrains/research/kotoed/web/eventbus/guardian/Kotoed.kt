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

fun kotoedPerAddressFilter(vertx: Vertx) = PerAddress(
        Address.Api.Submission.Code.Read to SubmissionReady(vertx, "submission_id"),
        Address.Api.Submission.Code.List to SubmissionReady(vertx, "submission_id"),
        Address.Api.Submission.Comments to SubmissionReady(vertx),
        Address.Api.Submission.CommentAggregates to SubmissionReady(vertx),
        Address.Api.Submission.Comment.Create to CommentCreateFilter(vertx),
        Address.Api.Submission.Comment.Update to CommentUpdateFilter(vertx),
        Address.Api.Submission.Result.Read to Permissive,
        Address.Api.Submission.Error to Permissive,
        Address.Api.Submission.Comment.Search to AuthorityRequired(Authority.Teacher),
        Address.Api.Submission.Comment.SearchCount to AuthorityRequired(Authority.Teacher),
        Address.Api.Project.Search to Permissive,
        Address.Api.Project.SearchCount to Permissive,
        Address.Api.Notification.RenderCurrent to Permissive,
        Address.Api.Notification.Current to Permissive,
        Address.Api.Course.Search to Permissive,
        Address.Api.Course.SearchCount to Permissive,
        Address.Api.Course.Create to AuthorityRequired(Authority.Teacher)
)

val KotoedPerAddressAnonymousFilter = PerAddress(
        Address.Api.OAuthProvider.List to Permissive
)

class KotoedFilter(vertx: Vertx): BridgeEventFilter {
    private val perAddress = kotoedPerAddressFilter(vertx)
    private val perAddressAnonymous = KotoedPerAddressAnonymousFilter
    private val underlying = AnyOf(HarmlessTypes,
            (Send and perAddressAnonymous),
            (LoginRequired and (Send and perAddress)))

    suspend override fun isAllowed(be: BridgeEvent): Boolean = underlying.isAllowed(be)

    fun makePermittedOptions() = perAddress.makePermittedOptions() + perAddressAnonymous.makePermittedOptions()
}

val KotoedPerAddressPatcher = PerAddressPatcher(
        Address.Api.Submission.Comment.Create to CommentCreatePatcher,
        Address.Api.Notification.RenderCurrent to NotificationPatcher
)

val KotoedPatcher = KotoedPerAddressPatcher
