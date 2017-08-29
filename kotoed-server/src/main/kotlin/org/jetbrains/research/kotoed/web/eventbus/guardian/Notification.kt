package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.notificationByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

object NotificationPatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val principalId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage ?: return

        rawMessage["body", "denizen_id"] = principalId

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "NotificationPatcher"
    }
}

data class ShouldBeNotificationTarget(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean {
        val principalId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val nId = rawMessage["body", "id"] as? Number ?: return false

        return vertx.eventBus().notificationByIdOrNull(nId.toInt())?.denizenId == principalId
    }
}
