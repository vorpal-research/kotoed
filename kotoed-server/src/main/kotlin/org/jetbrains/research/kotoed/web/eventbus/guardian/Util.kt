package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult

abstract class LoggingBridgeEventFilter : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean =
            checkIsAllowed(be).also { logResult(be, it) }

    suspend abstract fun checkIsAllowed(be: BridgeEvent): Boolean
}
