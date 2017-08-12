package org.jetbrains.research.kotoed.web.routers

import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.BridgeGuardian
import org.jetbrains.research.kotoed.web.eventbus.EventBusBridge
import org.jetbrains.research.kotoed.web.eventbus.guardian.KotoedFilter
import org.jetbrains.research.kotoed.web.eventbus.guardian.KotoedPatcher

@HandlerFactoryFor(UrlPattern.EventBus)
@LoginRequired
@JsonResponse
fun eventBusHandlerFactory(cfg: RoutingConfig) = with(cfg) {
    val filter = KotoedFilter(vertx)

    val bo = BridgeOptions().apply {
        for (po in filter.makePermittedOptions())
            addInboundPermitted(po)
    }
    EventBusBridge(vertx, bo, BridgeGuardian(filter, KotoedPatcher))
}