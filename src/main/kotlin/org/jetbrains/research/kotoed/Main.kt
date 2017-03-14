package org.jetbrains.research.kotoed

import io.vertx.core.Vertx
import io.vertx.ext.web.Router

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(KotoedServer::class.qualifiedName)
}

class KotoedServer : io.vertx.core.AbstractVerticle() {
    override fun start() {

        val router = Router.router(vertx)

        router.route("/").handler({ routingContext ->
            routingContext.response()
                    .putHeader("content-type", "text/plain")
                    .end("Kotoed online...")
        })

        vertx.createHttpServer()
                .requestHandler({ router.accept(it) })
                .listen(9000)
    }
}
