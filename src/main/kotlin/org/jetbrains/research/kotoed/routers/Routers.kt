package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpHeaderNames
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.util.*

@HandlerFor("/")
fun RoutingContext.handleIndex() = with(response()) {
    putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.HTML)
            .end(createHTML().html {
                head { title("The awesome kotoed") }
                body {
                    h2 { +"Kotoed here"; +Entities.copy }
                    p { a(href = "/teamcity") { +"Teamcity bindings" } }
                    p { a(href = "http://petstore.swagger.io/?url=https://bitbucket.org/vorpal-research/kotoed/raw/tip/docs/swagger.yaml") {
                            +"REST API documentation"
                        }
                    }
                }
            })
}

@HandlerFor("/teamcity/:address")
suspend fun RoutingContext.handleTeamcity() {
    val vertx = vertx()
    val eb = vertx.eventBus()

    val req = request()
    val address by req
    val body = if (req.method() == HttpMethod.POST) {
        req.bodyAsync().toJsonObject()
    } else throw IllegalArgumentException("Only POST is supported")

    val res = eb.sendAsync<JsonObject>(
            address.orEmpty(),
            body
    )

    jsonResponse()
            .end(res.body())
}
