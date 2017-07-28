package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.Loggable

interface BridgeEventPatcher : Loggable {
    fun patch(be: BridgeEvent)

    companion object {
        fun noop() = Noop

        fun all(vararg patchers: BridgeEventPatcher) = ApplyAll(*patchers)
    }
}

fun BridgeEventPatcher.logPatch(be: BridgeEvent) {
    log.trace("Bridge event ${be.type()} (${be.rawMessage}) is patched by {$this}")
}