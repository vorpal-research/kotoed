package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.jetbrains.research.kotoed.util.RouteProto

class EventBusBridge(
        vertx: Vertx,
        bridgeOptions: BridgeOptions,
        beHandler: Handler<BridgeEvent>

) : Handler<RoutingContext> {
    val sockJSHandler: SockJSHandler = SockJSHandler.create(vertx)
     init {
         sockJSHandler.bridge(bridgeOptions, beHandler)
     }
    override fun handle(event: RoutingContext) {
        sockJSHandler.handle(event)
    }
}