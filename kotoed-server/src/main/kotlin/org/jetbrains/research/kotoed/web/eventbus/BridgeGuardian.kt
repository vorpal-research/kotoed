package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.Handler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.newRequestUUID
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

class BridgeGuardian(val filter: BridgeEventFilter = BridgeEventFilter.intolerant(),
                     val patcher: BridgeEventPatcher = BridgeEventPatcher.noop()) :
        Handler<BridgeEvent>, Loggable {
    override fun handle(be: BridgeEvent) {
        launch(UnconfinedWithExceptions(this) + CoroutineName(newRequestUUID())) coro@ {
            if (!filter.isAllowed(be)) {
                be.complete(false)
                return@coro
            }

            patcher.patch(be)

            be.complete(true)
        }
    }
}