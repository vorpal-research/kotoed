package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.setStatus
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.Manifest.Index)
fun manifestHandler(context: RoutingContext) {
    val resp = context.response()
    resp.setStatus(HttpResponseStatus.OK)
            .putHeader("content-type", "application/json")
            .putHeader("accept-ranges", "bytes")
            .sendFile("webmanifest/manifest.json", 0L)
}
