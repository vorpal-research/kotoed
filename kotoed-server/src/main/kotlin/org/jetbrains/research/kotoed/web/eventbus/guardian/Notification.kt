package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.scope
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.notificationByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

object DenizenDatabaseIdPatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val principalId = be.socket().webUser()?.principal()?.get("id") ?: return
        val rawMessage = be.rawMessage ?: return

        rawMessage["body", "denizen_id"] = principalId

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "DenizenDatabaseIdPatcher"
    }
}

data class ShouldBeNotificationTarget(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean {
        val principalId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val nId = rawMessage["body", "id"] as? Number ?: return false

        return vertx.scope.notificationByIdOrNull(nId.toInt())?.denizenId == principalId
    }
}
