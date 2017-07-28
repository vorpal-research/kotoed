package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent

class Inverted(private val filter: BridgeEventFilter) : BridgeEventFilter {
    override suspend fun isAllowed(be: BridgeEvent): Boolean = !filter.isAllowed(be).also { logResult(be, it) }

    override fun toString(): String {
        return "Inverted($filter)"
    }
}