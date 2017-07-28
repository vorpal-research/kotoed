package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.Handler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

class BridgeGuardian(val filter: BridgeEventFilter = BridgeEventFilter.intolerant(),
                     val patcher: BridgeEventPatcher = BridgeEventPatcher.noop()) : Handler<BridgeEvent> {
    override fun handle(be: BridgeEvent) {
        if (!filter.isAllowed(be)) {
            be.complete(false)
            return
        }
        patcher.patch(be)

        be.complete(true)
    }
}