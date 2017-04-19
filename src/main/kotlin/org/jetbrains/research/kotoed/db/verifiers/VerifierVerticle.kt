package org.jetbrains.research.kotoed.db.verifiers

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.db.DatabaseVerticle
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.DimensionLocator
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.teamcity.util.plus
import org.jetbrains.research.kotoed.teamcity.util.putDefaultTCHeaders
import org.jetbrains.research.kotoed.util.*
import org.jooq.Table
import org.jooq.UpdatableRecord
import java.util.concurrent.TimeUnit

abstract class VerifierVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val verifyAddress = Address.DB.verify(entityName)

    val cache: Cache<Int, Boolean> = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES) // TODO: Move to settings
            .build<Int, Boolean>()

    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                verifyAddress,
                this::handleVerify
        )
    }

    open fun handleVerify(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        val res = cache[id] ?: verify(db { selectById(id) })
        msg.reply(JsonObject("status" to res))
    }.ignore()

    suspend abstract fun verify(data: JsonObject?): Boolean

}

class CourseVerifierVerticle : VerifierVerticle<CourseRecord>(Tables.COURSE) {
    suspend override fun verify(data: JsonObject?): Boolean {
        val wc = WebClient.create(vertx)

        data ?: return false
        // FIXME akhin Write event info to DB

        val buildtemplateid: String by data.delegate

        val url = TeamCityApi.BuildTypes +
                DimensionLocator.from("id", buildtemplateid)

        val res = vxa<HttpResponse<Buffer>> {
            wc.get(Config.TeamCity.Port, Config.TeamCity.Host, url)
                    .putDefaultTCHeaders()
                    .send(it)
        }

        if (HttpResponseStatus.OK.code() == res.statusCode()) {
            val baj = res.bodyAsJsonObject()

            val isTemplate = baj.getBoolean("templateFlag", false)

            if (isTemplate) return true
        }

        // FIXME akhin Write event info to DB

        return false
    }
}
