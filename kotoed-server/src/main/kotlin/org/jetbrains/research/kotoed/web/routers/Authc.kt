package org.jetbrains.research.kotoed.web.routers

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.HandlerFor
import org.jetbrains.research.kotoed.util.JsBundle
import org.jetbrains.research.kotoed.util.Templatize

@HandlerFor("/login")
@Templatize("login.jade")
@JsBundle("hello")
fun login(context: RoutingContext) {}

