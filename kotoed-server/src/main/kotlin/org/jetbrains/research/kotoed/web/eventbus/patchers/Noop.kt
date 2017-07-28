package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object Noop : BridgeEventPatcher {
    override fun patch(be: BridgeEvent) { logPatch(be) }

    override fun toString(): String {
        return "Noop"
    }
}