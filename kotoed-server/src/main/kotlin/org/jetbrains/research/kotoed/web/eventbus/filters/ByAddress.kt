package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get

abstract class ByAddress: BridgeEventFilter {
    suspend abstract fun isAllowed(principal: JsonObject, address: String): Boolean

    suspend override fun isAllowed(be: BridgeEvent): Boolean {
        val address = be.rawMessage?.get("address") as? String
        address ?: return false
        val principal = be.socket().webUser()?.principal() ?: return false
        return isAllowed(principal, address).also { logResult(be, it) }
    }

}