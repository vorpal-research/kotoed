package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import java.util.*

class ApplyAll(private vararg val patchers: BridgeEventPatcher): BridgeEventPatcher {
    override fun patch(be: BridgeEvent) {
        for (patcher in patchers)
            patcher.patch(be)
    }

    override fun toString(): String {
        return "ApplyAll(${Arrays.toString(patchers)})"
    }
}