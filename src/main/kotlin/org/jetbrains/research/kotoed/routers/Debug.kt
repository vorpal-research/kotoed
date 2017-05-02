package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.impl.DSL
import org.jooq.util.postgres.PostgresDataType

// XXX: testing, remove in production

@HandlerFor("/debug/*")
fun RoutingContext.handleDebug() {
    if (request().connection().run { localAddress().host() == remoteAddress().host() }) {
        next()
    } else {
        response()
                .end(HttpResponseStatus.FORBIDDEN)
    }
}

@HandlerFor("/debug/settings")
fun RoutingContext.handleDebugSettings() = jsonResponse().end(Config)

@HandlerFor("/debug/request")
fun RoutingContext.handleDebugRequest() {
    val req = request()
    val result = object : Jsonable {
        val headers = req.headers()
        val uri = req.absoluteURI()
        val method = req.rawMethod()
        val form = req.formAttributes()
        val params = req.params()
        val connection = object : Jsonable {
            val localAddress = req.connection().localAddress().toString()
            val remoteAddress = req.connection().remoteAddress().toString()
        }
    }
    jsonResponse().end(result)
}

@HandlerFor("/debug/crash/now")
fun RoutingContext.handleDebugCrashNow(): Unit = throw IllegalStateException("Forced crash")

@HandlerFor("/debug/crash/suspend")
suspend fun RoutingContext.handleDebugCrashSuspend(): Unit = throw IllegalStateException("Forced crash")

@HandlerFor("/debug/crash/delay")
suspend fun RoutingContext.handleDebugCrash() {
    val vertx = vertx()
    vertx.delayAsync(500)
    handleDebugCrashNow()
}

@HandlerFor("/debug/database/create")
fun RoutingContext.handleDebugDatabaseCreate() {
    val vertx = vertx()

    val ds = vertx.getSharedDataSource()

    val q = jooq(ds).use {
        it.createTableIfNotExists("debug")
                .column("id", PostgresDataType.SERIAL)
                .column("payload", PostgresDataType.JSON)
                .constraint(DSL.constraint("PK_DEBUG").primaryKey("id"))
                .execute()
    }
    val res = object : Jsonable {
        val query = q.toString()
    }
    jsonResponse().end(res)
}

@HandlerFor("/debug/database/fill")
suspend fun RoutingContext.handleDebugDatabaseFill() {
    val vertx = vertx()

    val ds = vertx.getSharedDataSource()

    jooq(ds).use {

        it.createTableIfNotExists("debug")
                .column("id", PostgresDataType.SERIAL)
                .column("payload", PostgresDataTypeEx.JSONB)
                .executeKAsync()

        with(Tables.DEBUG) {
            it.insertInto(this)
                    .columns(PAYLOAD)
                    .values(2)
                    .executeKAsync()

            it.insertInto(this)
                    .columns(PAYLOAD)
                    .values("Hello")
                    .executeKAsync()

            it.insertInto(this)
                    .columns(PAYLOAD)
                    .values(JsonObject("k" to 2, "f" to listOf(1, 2, 3)))
                    .executeKAsync()
        }

    }

    data class DebugType(val id: Int, val payload: Any?) : Jsonable

    val res = jooq(ds).use {
        with(Tables.DEBUG) {
            it.select(ID, PAYLOAD)
                    .from(this)
                    .fetchKAsync()
                    .into(DebugType::class.java)
                    .map { it.toJson() }
        }
    }

    vertx.goToEventLoop()

    jsonResponse().end(JsonArray(res).encodePrettily())
}

@HandlerFor("/debug/database/read/:id")
suspend fun RoutingContext.handleDebugDatabaseRead() {
    val vertx = vertx()

    val ds = vertx.getSharedDataSource()

    val id by request()

    val res =
            jooq(ds).use {
                it.createTableIfNotExists("debug")
                        .column("id", PostgresDataType.SERIAL)
                        .column("payload", PostgresDataTypeEx.JSONB)
                        .executeKAsync()

                with(Tables.DEBUG) {
                    it.select(PAYLOAD)
                            .from(this)
                            .where(ID.eq(id?.toInt()))
                            .fetchKAsync()
                            .map { it[PAYLOAD] }
                            .firstOrNull()
                }
            }

    vertx.goToEventLoop()

    jsonResponse().end(Json.encode(res))
}

@HandlerFor("/debug/database/clear")
suspend fun RoutingContext.handleDebugDatabaseClear() {
    val vertx = vertx()

    val ds = vertx.getSharedDataSource()

    jooq(ds).use {
        it.createTableIfNotExists("debug")
                .column("id", PostgresDataType.SERIAL)
                .column("payload", PostgresDataTypeEx.JSONB)
                .executeKAsync()

        it.truncate(Tables.DEBUG)
                .executeKAsync()
    }

    vertx.goToEventLoop()

    jsonResponse().end(JsonObject("success" to true))
}

@HandlerFor("/debug/eventbus/:address")
suspend fun RoutingContext.handleDebugEventBus() {
    val vertx = vertx()

    val eb = vertx.eventBus()
    val req = request()
    val address by req
    address ?: throw IllegalArgumentException("Missing event bus address in: $req")

    val body =
            if (req.method() == HttpMethod.POST) {
                req.bodyAsync().toJsonObject()
            } else {
                req.params()
                        .filterNot { (k, _) -> k == "address" }
                        .map { (k, v) -> Pair(k, JsonEx.decode(v)) }
                        .let(::JsonObject)
            }
    val res = eb.sendAsync<Any>(address, body)
    jsonResponse().end("${res.body()}")
}
