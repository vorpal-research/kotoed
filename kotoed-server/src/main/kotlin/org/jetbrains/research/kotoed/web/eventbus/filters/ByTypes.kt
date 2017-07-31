package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import java.util.*

class ByTypes(vararg val types: BridgeEventType) : BridgeEventFilter {
    override suspend fun isAllowed(be: BridgeEvent): Boolean = (be.type() in types).also { logResult(be, it) }

    override fun toString(): String {
        return "ByTypes(type=${Arrays.toString(types)})"
    }
}
