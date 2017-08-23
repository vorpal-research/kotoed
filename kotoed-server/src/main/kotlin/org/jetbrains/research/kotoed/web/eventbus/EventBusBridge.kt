package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler

class EventBusBridge(
        vertx: Vertx,
        bridgeOptions: BridgeOptions,
        beHandler: Handler<BridgeEvent>

) : Handler<RoutingContext> {
    private val sockJSHandler: SockJSHandler = SockJSHandler.create(vertx)
     init {
         sockJSHandler.bridge(bridgeOptions, beHandler)
     }
    override fun handle(event: RoutingContext) {
        sockJSHandler.handle(event)
    }
}