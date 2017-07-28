package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object Permissive: BridgeEventFilter {
    override fun isAllowed(be: BridgeEvent) = true.also { logResult(be, it) }

    override fun toString(): String = "Permissive"
}