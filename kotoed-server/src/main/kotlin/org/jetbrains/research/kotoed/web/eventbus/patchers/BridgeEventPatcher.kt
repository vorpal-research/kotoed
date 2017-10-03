package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.removeFields
import org.jetbrains.research.kotoed.util.truncateAt
import org.jetbrains.research.kotoed.web.eventbus.guardian.cleanUpBody

interface BridgeEventPatcher : Loggable {
    suspend fun patch(be: BridgeEvent)

    companion object {
        fun noop() = Noop

        fun all(vararg patchers: BridgeEventPatcher) = ApplyAll(*patchers)
    }
}

fun BridgeEventPatcher.logPatch(be: BridgeEvent) {
    log.trace("Bridge event ${be.type()} (${be.rawMessage.cleanUpBody().toString().truncateAt(500)}) is patched by $this")
}