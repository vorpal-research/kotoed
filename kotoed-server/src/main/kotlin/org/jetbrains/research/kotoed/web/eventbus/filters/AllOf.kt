package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import java.util.*

class AllOf(private vararg val ebs: BridgeEventFilter) : BridgeEventFilter {
    override fun isAllowed(be: BridgeEvent) = ebs.all { it.isAllowed(be) }.also { logResult(be, it) }

    override fun toString(): String {
        return "AllOf(${Arrays.toString(ebs)})"
    }
}