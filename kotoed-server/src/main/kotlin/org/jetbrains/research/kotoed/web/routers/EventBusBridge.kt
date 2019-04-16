package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.BridgeGuardian
import org.jetbrains.research.kotoed.web.eventbus.EventBusBridge
import org.jetbrains.research.kotoed.web.eventbus.guardian.KotoedFilter
import org.jetbrains.research.kotoed.web.eventbus.guardian.kotoedPatcher

@HandlerFactoryFor(UrlPattern.EventBus)
@NoBodyHandler
@EnableSessions
@JsonResponse
fun eventBusHandlerFactory(cfg: RoutingConfig) = with(cfg) {
    val filter = KotoedFilter(vertx)

    val bo = BridgeOptions().apply {
        for (po in filter.makePermittedOptions())
            addInboundPermitted(po)

        // FIXME belyaev: make this a nice method
        addOutboundPermitted(PermittedOptions().apply { addressRegex = ".*" })

    }
    EventBusBridge(vertx, bo, BridgeGuardian(vertx, filter, kotoedPatcher(vertx)))
}
