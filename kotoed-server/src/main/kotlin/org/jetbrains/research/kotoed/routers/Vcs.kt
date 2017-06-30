package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@HandlerFor("/vcs/ping/:vcs")
suspend fun RoutingContext.handleVcsPing() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()

    val url by req
    val vcs by req

    val message = object : Jsonable {
        val vcs = vcs
        val url = url.orEmpty().unquote()
    }

    val res = eb.sendAsync(Address.Code.Ping, message.toJson())

    jsonResponse().end(res.body())
}

@HandlerFor("/vcs/clone/:vcs")
suspend fun RoutingContext.handleVcsClone() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()

    val url by req
    val vcs by req

    val message = object : Jsonable {
        val vcs = vcs
        val url = url.orEmpty().unquote()
    }

    val res = eb.sendAsync(Address.Code.Download, message.toJson())

    jsonResponse().end(res.body())
}

@HandlerFor("""\/vcs\/read\/([^\/]+)\/(.+)""", isRegex = true)
suspend fun RoutingContext.handleVcsRead() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()

    val param0 by req
    val uid = param0
    val param1 by req
    val path = param1
    val revision by req

    val message = object : Jsonable {
        val uid = uid
        val path = path
        val revision = revision
    }

    val res = eb.sendAsync(Address.Code.Read, message.toJson())

    if (res.body().getBoolean("success")) {
        jsonResponse().end(res.body())
    } else {
        jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}

@HandlerFor("/vcs/list/:uid")
suspend fun RoutingContext.handleVcsList() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()

    val uid by req
    val revision by req

    val message = object : Jsonable {
        val uid = uid
        val revision = revision
    }

    val res = eb.sendAsync(Address.Code.List, message.toJson())

    if (res.body().getBoolean("success")) {
        jsonResponse().end(res.body())
    } else {
        jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}

@HandlerFor("/vcs/diff/:uid/:from::to")
suspend fun RoutingContext.handleVcsDiff() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()

    val uid by req
    val path by req
    val from by req
    val to by req

    val message = object : Jsonable {
        val uid = uid
        val path = path
        val from = from
        val to = to
    }

    val res = eb.sendAsync(Address.Code.Diff, message.toJson())

    if (res.body().getBoolean("success")) {
        jsonResponse().end(res.body())
    } else {
        jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}
