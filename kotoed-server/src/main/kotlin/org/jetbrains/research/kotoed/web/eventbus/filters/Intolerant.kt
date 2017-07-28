package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object Intolerant : BridgeEventFilter {
    override fun isAllowed(be: BridgeEvent) = false

    override fun toString(): String {
        return "BridgeEventPatcher.intolerant()"
    }
}