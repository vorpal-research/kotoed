package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.setStatus
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.CacheWorker.Index)
fun cacheWorker(context: RoutingContext) {
    val resp = context.response()
    resp.setStatus(HttpResponseStatus.OK)
            .putHeader("content-type", "application/javascript")
            .putHeader("accept-ranges", "bytes")
            .sendFile("webroot/static/cachingWorker.js", 0L)
}
