package org.jetbrains.research.kotoed

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.util.vx

fun main(args: Array<String>) {
    launch(Unconfined) {
        val vertx = vx<Vertx> { Vertx.clusteredVertx(VertxOptions(), it) }

        vertx.deployVerticle(KotoedServer::class.qualifiedName)
    }
}

class KotoedServer : io.vertx.core.AbstractVerticle() {
    override fun start() {
        launch(Unconfined) {
            val router = Router.router(vertx)

            val gsms = vx<AsyncMap<String, String>> { vertx.sharedData().getClusterWideMap("gsms", it) }

            router.route("/").handler({ ctx ->
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Kotoed online...")
            })

            router.route("/global/create/:key/:value").handler { ctx ->
                val key = ctx.request().getParam("key")
                val value = ctx.request().getParam("value")
                launch(Unconfined) {
                    vx { gsms.put(key, value, it) }
                    ctx.response()
                            .putHeader("content-type", "text/plain")
                            .end("CREATED: $key -> $value")
                }
            }

            router.route("/global/read/:key").handler { ctx ->
                val key = ctx.request().getParam("key")
                launch(Unconfined) {
                    val value = vx<String> { gsms.get(key, it) }
                    ctx.response()
                            .putHeader("content-type", "text/plain")
                            .end("READ: $key -> $value")
                }
            }

            vertx.createHttpServer()
                    .requestHandler({ router.accept(it) })
                    .listen(9000)
        }
    }
}
