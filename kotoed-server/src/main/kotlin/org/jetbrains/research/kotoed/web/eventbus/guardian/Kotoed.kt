package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.web.eventbus.filters.*
import org.jetbrains.research.kotoed.web.eventbus.patchers.PerAddressPatcher

val HarmlessTypes =
        ByTypes(BridgeEventType.RECEIVE,
                BridgeEventType.SOCKET_IDLE,
                BridgeEventType.SOCKET_PING,
                BridgeEventType.SOCKET_CREATED)

val Send = ByType(BridgeEventType.SEND)

fun KotoedPerAddressFilter(vertx: Vertx) = PerAddress(
        Address.Api.Submission.Code.Read to BridgeEventFilter.permissive(),
        Address.Api.Submission.Code.List to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comments to BridgeEventFilter.permissive(),
        Address.Api.Submission.CommentAggregates to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comment.Create to CommentCreateFilter,
        Address.Api.Submission.Comment.Update to CommentUpdateFilter(vertx)
)

class KotoedFilter(vertx: Vertx): BridgeEventFilter {
    private val perAddress = KotoedPerAddressFilter(vertx)
    private val underlying = LoginRequired and (HarmlessTypes or (Send and perAddress))

    suspend override fun isAllowed(be: BridgeEvent): Boolean = underlying.isAllowed(be)

    fun makePermittedOptions() = perAddress.makePermittedOptions()
}

val KotoedPerAddressPatcher = PerAddressPatcher(
        Address.Api.Submission.Comment.Create to CommentCreatePatcher
)

val KotoedPatcher = KotoedPerAddressPatcher