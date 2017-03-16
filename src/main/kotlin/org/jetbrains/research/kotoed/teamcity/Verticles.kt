package org.jetbrains.research.kotoed.teamcity

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.project.Create
import org.jetbrains.research.kotoed.data.teamcity.project.Test
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.util.*

class TeamCityVerticle : AbstractVerticle() {
    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                Address.TeamCity.Test,
                this@TeamCityVerticle::consumeTeamCityTest.withExceptions()
        )

        eb.consumer<JsonObject>(
                Address.TeamCity.Create,
                this@TeamCityVerticle::consumeTeamCityCreate.withExceptions()
        )
    }

    fun consumeTeamCityTest(msg: Message<JsonObject>) {
        val wc = WebClient.create(vertx)

        val test = fromJson<Test>(msg.body())

        launch(UnconfinedWithExceptions(msg)) {
            val res = vxa<HttpResponse<Buffer>> {
                wc.get(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Projects)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .send(it)
            }
            msg.reply(res.bodyAsJsonObject())
        }
    }

    fun consumeTeamCityCreate(msg: Message<JsonObject>) {
        val wc = WebClient.create(vertx)

        val create = fromJson<Create>(msg.body())

        launch(UnconfinedWithExceptions(msg)) {
            val res = vxa<HttpResponse<Buffer>> {
                wc.post(Config.TeamCity.Port, Config.TeamCity.Host, TeamCityApi.Projects)
                        .putHeader(HttpHeaderNames.AUTHORIZATION, Config.TeamCity.AuthString)
                        .putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                        .sendBuffer(Buffer.buffer(create.name), it)
            }
            msg.reply(res.bodyAsJsonObject())
        }
    }
}
