package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get

class ShouldBeSelfForFilter(
        private val idPath: String = "denizenId"
) : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(idPath)
                ?: return false

        return be.socket().webUser().principal()["id"] == id
    }

    override fun toString(): String {
        return "ShouldBeSelfForFilter()"
    }
}