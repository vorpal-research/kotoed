package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.handler.StaticHandler
import org.jetbrains.research.kotoed.util.routing.HandlerFactoryFor
import org.jetbrains.research.kotoed.util.routing.RoutingConfig
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFactoryFor(UrlPattern.Static)
fun staticHandlerFactory(cfg: RoutingConfig) = StaticHandler.create(cfg.staticLocalPath)