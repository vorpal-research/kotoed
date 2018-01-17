package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.removeFields
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult

abstract class LoggingBridgeEventFilter : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean =
            checkIsAllowed(be).also { logResult(be, it) }

    suspend abstract fun checkIsAllowed(be: BridgeEvent): Boolean
}


fun JsonObject?.cleanUp(): JsonObject? {
    this ?: return this
    val msg = this.copy()
    return msg.removeFields("password", "initiatorPassword", "newPassword")

}

fun JsonObject?.cleanUpBody(): JsonObject? {
    this ?: return this
    val msg = this.copy()
    val body = msg?.get("body") as? JsonObject ?: return this

    body.cleanUp()

    return msg
}
