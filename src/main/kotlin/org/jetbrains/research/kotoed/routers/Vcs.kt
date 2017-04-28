package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@HandlerFor("/vcs/ping/:vcs")
fun handleVcsPing(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()

    val url by req
    val vcs by req

    val message = object : Jsonable {
        val vcs = vcs
        val url = url.orEmpty().unquote()
    }

    val res = eb.sendAsync(Address.Code.Ping, message.toJson())

    ctx.jsonResponse().end(res.body())
}

@HandlerFor("/vcs/clone/:vcs")
fun handleVcsClone(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()

    val url by req
    val vcs by req

    val message = object : Jsonable {
        val vcs = vcs
        val url = url.orEmpty().unquote()
    }

    val res = eb.sendAsync(Address.Code.Download, message.toJson())

    ctx.jsonResponse().end(res.body())
}

@HandlerFor("""\/vcs\/read\/([^\/]+)\/(.+)""", isRegex = true)
fun handleVcsRead(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()

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
        ctx.jsonResponse().end(res.body())
    } else {
        ctx.jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}

@HandlerFor("/vcs/list/:uid")
fun handleVcsList(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()

    val uid by req
    val revision by req

    val message = object : Jsonable {
        val uid = uid
        val revision = revision
    }

    val res = eb.sendAsync(Address.Code.List, message.toJson())

    if (res.body().getBoolean("success")) {
        ctx.jsonResponse().end(res.body())
    } else {
        ctx.jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}

@HandlerFor("/vcs/diff/:uid/:from::to")
fun handleVcsDiff(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()

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
        ctx.jsonResponse().end(res.body())
    } else {
        ctx.jsonResponse()
                .setStatus(HttpResponseStatus.NOT_FOUND)
                .end(HttpResponseStatus.NOT_FOUND.toJson()
                        .mergeIn(res.body()))
    }
}
