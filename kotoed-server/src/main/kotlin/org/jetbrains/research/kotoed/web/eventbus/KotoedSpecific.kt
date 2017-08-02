package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.filters.*
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher
import org.jetbrains.research.kotoed.web.eventbus.patchers.PerAddressPatcher

/******************************************************************************/

val HarmlessTypes =
        ByTypes(BridgeEventType.RECEIVE,
                BridgeEventType.SOCKET_IDLE,
                BridgeEventType.SOCKET_PING,
                BridgeEventType.SOCKET_CREATED)

val Send = ByType(BridgeEventType.SEND)

val KotoedPerAddressFilter = PerAddress(
        Address.Api.Submission.Code.Read to BridgeEventFilter.permissive(),
        Address.Api.Submission.Code.List to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comments to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comment.Create to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comment.Update to BridgeEventFilter.permissive()
)

val KotoedFilter = LoginRequired and (HarmlessTypes or (Send and KotoedPerAddressFilter))

/******************************************************************************/

object CommentCreatePatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val authorId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val body = rawMessage["body"] as? JsonObject ?: return

        body["authorId"] = authorId
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "CommentCreatePatcher"
    }
}

val KotoedPerAddressPatcher = PerAddressPatcher(
        Address.Api.Submission.Comment.Create to CommentCreatePatcher
)

val KotoedPatcher = KotoedPerAddressPatcher