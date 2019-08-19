package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.research.kotoed.util.LogExceptions
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.newRequestUUID
import org.jetbrains.research.kotoed.util.truncateAt
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.guardian.cleanUpBody
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

class BridgeGuardian(val vertx: Vertx,
                     val filter: BridgeEventFilter = BridgeEventFilter.intolerant(),
                     val patcher: BridgeEventPatcher = BridgeEventPatcher.noop()) :
        Handler<BridgeEvent>, Loggable {
    override fun handle(be: BridgeEvent) {
        var context = vertx.dispatcher() + LogExceptions()
        if (be.rawMessage != null) {
            val uuid = newRequestUUID()
            log.trace("Assigning $uuid to ${be.rawMessage.cleanUpBody().toString().truncateAt(500)}\n" +
                    "from principal ${be.socket()?.webUser()?.principal()}")
            context += CoroutineName(uuid)
        }

        CoroutineScope(context).launch coro@ {
            if (!filter.isAllowed(be)) {
                be.complete(false)
                return@coro
            }

            patcher.patch(be)

            be.complete(true)
        }
    }
}
