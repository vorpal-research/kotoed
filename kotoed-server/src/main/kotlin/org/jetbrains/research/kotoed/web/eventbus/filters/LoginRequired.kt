package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object LoginRequired : BridgeEventFilter {
    override fun isAllowed(be: BridgeEvent): Boolean =
            (be.socket().webSession() != null &&  !be.socket().webSession().isDestroyed).also { logResult(be, it) }

    override fun toString(): String = "LoginRequired"
}