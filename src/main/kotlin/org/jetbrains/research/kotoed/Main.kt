package org.jetbrains.research.kotoed

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.TeamCityVerticle
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.eventbus.sendAsync
import java.util.*

fun main(args: Array<String>) {
    launch(Unconfined) {
        val vertx = vx<Vertx> { Vertx.clusteredVertx(VertxOptions(), it) }

        vertx.deployVerticle(RootVerticle::class.qualifiedName)
        vertx.deployVerticle(TeamCityVerticle::class.qualifiedName)
    }
}

typealias GSMS_TYPE = AsyncMap<String, String>
const val GSMS_ID = "gsms"

class RootVerticle : io.vertx.core.AbstractVerticle(), Loggable {

    override fun start() {
        launch(Unconfined) {
            val router = Router.router(vertx)

            val gsms = vx<GSMS_TYPE> { vertx.sharedData().getClusterWideMap("gsms", it) }
            router.route()
                    .handler { ctx -> ctx.put(GSMS_ID, gsms).next() }

            router.route("/")
                    .handler(this@RootVerticle::handleIndex)

            router.route("/global/create/:key/:value")
                    .handler(this@RootVerticle::handleGsmsCreate)
            router.route("/global/read/:key")
                    .handler(this@RootVerticle::handleGsmsRead)

            router.route("/teamcity")
                    .handler(this@RootVerticle::handleTeamcity)

            vertx.createHttpServer()
                    .requestHandler({ router.accept(it) })
                    .listen(Config.Root.Port)
        }
    }

    fun handleIndex(ctx: RoutingContext) {
        ctx.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, "text/html")
                .end(createHTML().html{
                    head { title("The awesome kotoed") }
                    body {
                        h2{ +"Kotoed here"; +Entities.copy }
                        p{ a(href = "/global/create/kotoed/${Random().nextInt()}"){ +"Create stuff" } }
                        p{ a(href = "/global/read/kotoed"){ +"Read stuff" } }
                        p{ a(href = "/teamcity"){ +"Teamcity bindings" } }

                        this.putButton()
                    }
                })
    }

    fun handleGsmsCreate(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        val key by ctx.request()
        val value by ctx.request()

        data class Display(val type: String, val key: String, val value: String): Jsonable

        launch(Unconfined) {
            vx { gsms.put(key, value, it) }
            ctx.response()
                    .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                    .end(Display("created", key, value).toJson().encodePrettily())
        }
    }

    fun handleGsmsRead(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        launch(Unconfined) {
            val key by ctx.request() // moving this line above launch crashes Kotlin runtime :)
                                     // https://youtrack.jetbrains.com/issue/KT-16864
            val value = vx<String> { gsms.get(key, it) }

            val response = object: Jsonable {
                val type = "read"
                val key = key.toString()
                val value = value.toString()
            }

            ctx.response()
                    .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                    .end(response.toJson().encodePrettily())
        }
    }

    fun handleTeamcity(ctx: RoutingContext) {
        val eb = vertx.eventBus()

        launch(Unconfined) {
            val res = eb.sendAsync(
                Address.TeamCityVerticle,
                JsonObject().put(
                        "payload",
                        "/app/rest/projects"
                )
            )

            ctx.response()
                    .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                    .end(res.body().encodePrettily())
        }
    }
}
