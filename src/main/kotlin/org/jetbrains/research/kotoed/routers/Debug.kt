package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.impl.DSL
import org.jooq.util.postgres.PostgresDataType

// XXX: testing, remove in production

@HandlerFor("/debug/*")
fun handleDebug(ctx: RoutingContext) {
    if (ctx.request().connection().run { localAddress().host() == remoteAddress().host() }) {
        ctx.next()
    } else {
        ctx.response()
                .end(HttpResponseStatus.FORBIDDEN)
    }
}

@HandlerFor("/debug/settings")
fun handleDebugSettings(ctx: RoutingContext) {
    ctx.jsonResponse().end(Config)
}

@HandlerFor("/debug/request")
fun handleDebugRequest(ctx: RoutingContext) {
    val req = ctx.request()
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
    ctx.jsonResponse().end(result)
}

@HandlerFor("/debug/crash")
fun handleDebugCrash(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()
    vertx.delayAsync(500)
    throw IllegalStateException("Forced crash")
}

@HandlerFor("/debug/database/create")
fun handleDebugDatabaseCreate(ctx: RoutingContext) {
    val vertx = ctx.vertx()

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
    ctx.jsonResponse().end(res)
}

@HandlerFor("/debug/database/fill")
fun handleDebugDatabaseFill(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()

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

    ctx.jsonResponse().end(JsonArray(res).encodePrettily())
}

@HandlerFor("/debug/database/read/:id")
fun handleDebugDatabaseRead(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()

    val ds = vertx.getSharedDataSource()

    val id by ctx.request()

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

    ctx.jsonResponse().end(Json.encode(res))
}

@HandlerFor("/debug/database/clear")
fun handleDebugDatabaseClear(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()

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

    ctx.jsonResponse().end(JsonObject("success" to true))
}

@HandlerFor("/debug/eventbus/:address")
fun handleDebugEventBus(ctx: RoutingContext) = launch(UnconfinedWithExceptions(ctx)) {
    val vertx = ctx.vertx()

    val eb = vertx.eventBus()
    val req = ctx.request()
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
    val res = vxa<Message<Any>> { eb.send(address, body, it) }
    ctx.jsonResponse().end("${res.body()}")
}
