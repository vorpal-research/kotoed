package org.jetbrains.research.kotoed

import io.netty.handler.codec.http.HttpHeaderNames
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
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.teamcity.TeamCityVerticle
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.PostgresDataTypeEx
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.eventbus.sendAsync
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.jooq.util.postgres.PostgresDataType
import java.util.*

fun main(args: Array<String>) {
    Thread.currentThread().contextClassLoader.getResourceAsStream("system.properties").use {
        System.getProperties().load(it)
    }

    launch(Unconfined) {
        val vertx = clusteredVertxAsync()

        vertx.deployVerticle(RootVerticle::class.qualifiedName)
        vertx.deployVerticle(TeamCityVerticle::class.qualifiedName)
    }
}

private typealias GSMS_TYPE = AsyncMap<String, String>
private const val GSMS_ID = "gsms"

class RootVerticle : io.vertx.core.AbstractVerticle(), Loggable {

    override fun start() {
        launch(Unconfined) {
            val router = Router.router(vertx)

            log.info("Alive and standing")

            val gsms = vxa<GSMS_TYPE> { vertx.sharedData().getClusterWideMap("gsms", it) }
            router.route()
                    .handler { ctx -> ctx.put(GSMS_ID, gsms).next() }
                    .failureHandler(this@RootVerticle::handleFailure)

            router.route("/")
                    .handler(this@RootVerticle::handleIndex)

            router.route("/debug/*")
                    .handler(this@RootVerticle::handleDebug)
            router.route("/debug/settings")
                    .handler(this@RootVerticle::handleSettings)
            router.route("/debug/request")
                    .handler(this@RootVerticle::handleDebugRequest)
            router.route("/debug/database/create")
                    .handler(this@RootVerticle::handleDebugDatabaseCreate)
            router.route("/debug/database/fill")
                    .handler(this@RootVerticle::handleDebugDatabaseFill)


            router.route("/global/create/:key/:value")
                    .handler(this@RootVerticle::handleGsmsCreate)
            router.route("/global/read/:key")
                    .handler(this@RootVerticle::handleGsmsRead)

            router.route("/teamcity/:address")
                    .handler(this@RootVerticle::handleTeamcity)

            vertx.createHttpServer()
                    .requestHandler({ router.accept(it) })
                    .listen(Config.Root.Port)
        }
    }

    fun handleIndex(ctx: RoutingContext) = with(ctx.response()) {
        putHeader(HttpHeaderNames.CONTENT_TYPE, "text/html")
                .end(createHTML().html {
                    head { title("The awesome kotoed") }
                    body {
                        h2 { +"Kotoed here"; +Entities.copy }
                        p { a(href = "/global/create/kotoed/${Random().nextInt()}") { +"Create stuff" } }
                        p { a(href = "/global/read/kotoed") { +"Read stuff" } }
                        p { a(href = "/teamcity") { +"Teamcity bindings" } }
                    }
                })
    }

    // XXX: testing, remove in production
    fun handleDebug(ctx: RoutingContext) {
        if (ctx.request().connection().run { localAddress().host() == remoteAddress().host() }) ctx.next()
        else ctx.jsonResponse()
                .setStatusCode(HttpResponseStatus.FORBIDDEN.code())
                .setStatusMessage("Forbidden")
                .end(JsonObject("code" to 403, "message" to "forbidden"))
    }

    fun handleSettings(ctx: RoutingContext) {
        ctx.jsonResponse()
                .end(Config.toString())
    }

    fun handleDebugRequest(ctx: RoutingContext) {
        val req = ctx.request()
        val result = object: Jsonable {
            val headers = req.headers()
            val uri = req.absoluteURI()
            val method = req.rawMethod()
            val form = req.formAttributes()
            val params = req.params()
            val connection = object: Jsonable {
                val localAddress = req.connection().localAddress().toString()
                val remoteAddress = req.connection().remoteAddress().toString()
            }
        }
        ctx.jsonResponse().end(result.toJson())
    }

    fun handleDebugDatabaseCreate(ctx: RoutingContext) {
        val q = DSL.using("jdbc:postgresql://localhost/kotoed", "kotoed", "kotoed").use {
            it.createTableIfNotExists("debug")
                    .column("id", PostgresDataType.SERIAL)
                    .column("payload", PostgresDataType.JSON)
                    .constraint(DSL.constraint("PK_DEBUG").primaryKey("id"))
                    .execute()
        }
        val ret = object: Jsonable { val query = q.toString() }
        ctx.jsonResponse().end(ret.toJson())
    }

    fun handleDebugDatabaseFill(ctx: RoutingContext) {

        launch(Unconfined) {

            DSL.using("jdbc:postgresql://localhost/kotoed", "kotoed", "kotoed").use {

                it.createTableIfNotExists("debug")
                        .column("id", PostgresDataType.SERIAL)
                        .column("payload", PostgresDataTypeEx.JSONB)
                        .constraint(constraint("PK_DEBUG").primaryKey("id"))
                        .executeKAsync()

                it.insertInto(table("debug"))
                        .columns(field("payload", PostgresDataTypeEx.JSONB))
                        .values(2)
                        .executeKAsync()

                it.insertInto(table("debug"))
                        .columns(field("payload", PostgresDataTypeEx.JSONB))
                        .values("Hello")
                        .executeKAsync()

                it.insertInto(table("debug"))
                        .columns(field("payload", PostgresDataTypeEx.JSONB))
                        .values(JsonObject("k" to 2, "f" to listOf(1,2,3)))
                        .executeKAsync()
            }

            data class DebugType(val id: Int, val payload: Any?) : Jsonable

            val res = DSL.using("jdbc:postgresql://localhost/kotoed", "kotoed", "kotoed").use {
                it.select(field("id", PostgresDataType.INT), field("payload", PostgresDataTypeEx.JSONB))
                        .from(table("debug"))
                        .fetchKAsync()
                        .into(DebugType::class.java)
                        .map { it.toJson() }
            }

            vertx.goToEventLoop()

            ctx.jsonResponse().end(JsonArray(res).encodePrettily())
        }
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
            val body = if (ctx.request().method() == HttpMethod.POST) {
                vxt<Buffer> { ctx.request().bodyHandler(it) }.toJsonObject()
            } else {
                JsonObject()
            }
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
