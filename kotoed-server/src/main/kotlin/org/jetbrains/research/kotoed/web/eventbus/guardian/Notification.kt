package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

object NotificationPatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val principalId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val body = rawMessage["body"] as? JsonObject ?: return

        body["denizen_id"] = principalId
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "NotificationPatcher"
    }
}