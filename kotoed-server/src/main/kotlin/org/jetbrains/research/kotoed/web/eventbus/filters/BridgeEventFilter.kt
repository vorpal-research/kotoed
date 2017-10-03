package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.removeFields
import org.jetbrains.research.kotoed.util.truncateAt
import org.jetbrains.research.kotoed.web.eventbus.guardian.cleanUpBody

interface BridgeEventFilter : Loggable {
    suspend fun isAllowed(be: BridgeEvent): Boolean

    companion object {
        fun permissive() = Permissive

        fun intolerant() = Intolerant

        fun all(vararg ebs: BridgeEventFilter) = AllOf(*ebs)

        fun any(vararg ebs: BridgeEventFilter) = AnyOf(*ebs)
    }
}

infix fun BridgeEventFilter.and(other: BridgeEventFilter) = BridgeEventFilter.all(this, other)

infix fun BridgeEventFilter.or(other: BridgeEventFilter) = BridgeEventFilter.any(this, other)

operator fun BridgeEventFilter.not() = Inverted(this)

fun BridgeEventFilter.logResult(be: BridgeEvent, result: Boolean) {
    if (BridgeEventType.SOCKET_PING == be.type()) return
    log.trace("Bridge event ${be.type()} (${be.rawMessage.cleanUpBody().toString().truncateAt(500)}) " +
            "${if (result) "accepted" else "rejected"} " +
            "by $this filter"
    )
}
