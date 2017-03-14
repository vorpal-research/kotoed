package org.jetbrains.research.kotoed.teamcity

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.putHeader
import org.jetbrains.research.kotoed.util.vx

class TeamCityVerticle : AbstractVerticle() {
    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                Address.TeamCityVerticle,
                this@TeamCityVerticle::consumeKotoedTeamcity
        )
    }

    fun consumeKotoedTeamcity(msg: Message<JsonObject>) {
        val wc = WebClient.create(vertx)

        launch(Unconfined) {
            val res = vx<HttpResponse<Buffer>> {
                val payload = msg.body().getValue("payload")
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, payload.toString())
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.Basic)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .send(it)
            }
            msg.reply(res.bodyAsJsonObject())
        }
    }
}
