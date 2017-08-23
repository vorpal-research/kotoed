package org.jetbrains.research.kotoed.web.routers

import org.jetbrains.research.kotoed.util.routing.HandlerFactoryFor
import org.jetbrains.research.kotoed.util.routing.Order
import org.jetbrains.research.kotoed.util.routing.RoutingConfig
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFactoryFor(UrlPattern.Star)
@Order(-1000)
fun loggingHandlerFactory(cfg: RoutingConfig) = cfg.loggingHandler