package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpHeaderNames
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.util.*

@HandlerFor("/")
fun handleIndex(ctx: RoutingContext) = with(ctx.response()) {
    putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.HTML)
            .end(createHTML().html {
                head { title("The awesome kotoed") }
                body {
                    h2 { +"Kotoed here"; +Entities.copy }
                    p { a(href = "/teamcity") { +"Teamcity bindings" } }
                }
            })
}

@HandlerFor("/teamcity/:address")
fun handleTeamcity(ctx: RoutingContext) {
    val vertx = ctx.vertx()
    val eb = vertx.eventBus()

    val req = ctx.request()
    val address by req

    launch(UnconfinedWithExceptions(ctx)) {
        val body = if (req.method() == HttpMethod.POST) {
            req.bodyAsync().toJsonObject()
        } else throw IllegalArgumentException("Only POST is supported")

        val res = eb.sendAsync<JsonObject>(
                address.orEmpty(),
                body
        )

        ctx.jsonResponse()
                .end(res.body())
    }
}
