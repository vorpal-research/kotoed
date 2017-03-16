package org.jetbrains.research.kotoed

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.teamcity.TeamCityVerticle
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.eventbus.sendAsync

fun main(args: Array<String>) {
    launch(Unconfined) {
        val vertx = vxa<Vertx> { Vertx.clusteredVertx(VertxOptions(), it) }

        vertx.deployVerticle(RootVerticle::class.qualifiedName)
        vertx.deployVerticle(TeamCityVerticle::class.qualifiedName)
    }
}

private typealias GSMS_TYPE = AsyncMap<String, String>
private const val GSMS_ID = "gsms"

class RootVerticle : io.vertx.core.AbstractVerticle() {

    override fun start() {
        launch(Unconfined) {
            val router = Router.router(vertx)

            val gsms = vxa<GSMS_TYPE> { vertx.sharedData().getClusterWideMap("gsms", it) }
            router.route()
                    .handler { ctx -> ctx.put(GSMS_ID, gsms).next() }
                    .failureHandler(this@RootVerticle::handleFailure)

            router.route("/")
                    .handler(this@RootVerticle::handleIndex)

            router.route("/global/create/:key/:value")
                    .handler(this@RootVerticle::handleGsmsCreate)
            router.route("/global/read/:key")
                    .handler(this@RootVerticle::handleGsmsRead)

            router.route(HttpMethod.POST, "/teamcity/:address")
                    .handler(this@RootVerticle::handleTeamcity)

            vertx.createHttpServer()
                    .requestHandler({ router.accept(it) })
                    .listen(Config.Root.Port)
        }
    }

    fun handleIndex(ctx: RoutingContext) {
        ctx.jsonResponse()
                .end(
                        JsonObject(
                                "data" to "Kotoed online..."
                        )
                )
    }

    fun handleGsmsCreate(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        val key by ctx.request()
        val value by ctx.request()

        launch(Unconfined) {
            vxu { gsms.put(key, value, it) }
            ctx.jsonResponse()
                    .end(
                            JsonObject(
                                    "type" to "create",
                                    "key" to key,
                                    "value" to value
                            )
                    )
        }
    }

    fun handleGsmsRead(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        val key by ctx.request()

        launch(Unconfined) {
            val value = vxa<String> { gsms.get(key, it) }
            ctx.jsonResponse()
                    .end(
                            JsonObject(
                                    "type" to "read",
                                    "key" to key,
                                    "value" to value
                            )
                    )
        }
    }

    fun handleTeamcity(ctx: RoutingContext) {
        val eb = vertx.eventBus()

        val address by ctx.request()

        launch(Unconfined) {
            val body = vxt<Buffer> { ctx.request().bodyHandler(it) }.toJsonObject()
            val res = eb.sendAsync<JsonObject>(
                    address,
                    body
            )
            ctx.jsonResponse()
                    .end(res.body())
        }
    }

    fun handleFailure(ctx: RoutingContext) {
        val ex = ctx.failure()
        ctx.jsonResponse()
                .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .end(
                        JsonObject(
                                "error" to ex.message
                        )
                )
    }
}
