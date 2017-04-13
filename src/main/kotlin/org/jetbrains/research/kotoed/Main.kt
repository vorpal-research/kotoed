package org.jetbrains.research.kotoed

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.code.CodeVerticle
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.db.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.statistics.JUnitStatisticsVerticle
import org.jetbrains.research.kotoed.teamcity.verticles.ArtifactCrawlerVerticle
import org.jetbrains.research.kotoed.teamcity.verticles.TeamCityVerticle
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.jooq.util.postgres.PostgresDataType
import java.util.*

fun main(args: Array<String>) {
    launch(Unconfined) { startApplication(args) }
}

suspend fun startApplication(args: Array<String>): Vertx {
    Thread.currentThread().contextClassLoader.getResourceAsStream(
            System.getProperty("kotoed.systemPropertiesFile", "system.properties")
    ).use {
        System.getProperties().load(it)
    }

    val vertx = clusteredVertxAsync(
            VertxOptions().also {
                it.metricsOptions = DropwizardMetricsOptions(
                        enabled = Config.Debug.Metrics.Enabled,
                        jmxEnabled = Config.Debug.Metrics.Enabled
                )
            }
    )

    vertx.deployVerticle(RootVerticle::class.qualifiedName)
    vertx.deployVerticle(TeamCityVerticle::class.qualifiedName)
    vertx.deployVerticle(CodeVerticle::class.qualifiedName)
    vertx.deployVerticle(ArtifactCrawlerVerticle::class.qualifiedName)
    vertx.deployVerticle(JUnitStatisticsVerticle::class.qualifiedName)
    vertx.deployVerticle(DebugVerticle::class.qualifiedName)
    vertx.deployVerticle(DenizenVerticle::class.qualifiedName)
    vertx.deployVerticle(UserAuthVerticle::class.qualifiedName)
    vertx.deployVerticle(ProjectVerticle::class.qualifiedName)
    vertx.deployVerticle(CourseVerticle::class.qualifiedName)
    return vertx
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
            router.route("/debug/crash")
                    .handler(this@RootVerticle::handleDebugCrash)
            router.route("/debug/request")
                    .handler(this@RootVerticle::handleDebugRequest)
            router.route("/debug/database/create")
                    .handler(this@RootVerticle::handleDebugDatabaseCreate)
            router.route("/debug/database/fill")
                    .handler(this@RootVerticle::handleDebugDatabaseFill)
            router.route("/debug/database/clear")
                    .handler(this@RootVerticle::handleDebugDatabaseClear)
            router.route("/debug/database/read/:id")
                    .handler(this@RootVerticle::handleDebugDatabaseRead)
            router.route("/debug/eventbus/:address")
                    .handler(this@RootVerticle::handleDebugEventBus)

            router.route("/global/create/:key/:value")
                    .handler(this@RootVerticle::handleGsmsCreate)
            router.route("/global/read/:key")
                    .handler(this@RootVerticle::handleGsmsRead)

            router.route("/teamcity/:address")
                    .handler(this@RootVerticle::handleTeamcity)

            router.route("/vcs/ping/:vcs")
                    .handler(this@RootVerticle::handleVCSPing)
            router.route("/vcs/clone/:vcs")
                    .handler(this@RootVerticle::handleVCSClone)
            router.routeWithRegex("""\/vcs\/read\/([^\/]+)\/(.+)""")
                    .handler(this@RootVerticle::handleVCSRead)
            router.route("/vcs/diff/:uid/:from::to")
                    .handler(this@RootVerticle::handleVCSDiff)
            router.route("/vcs/list/:uid")
                    .handler(this@RootVerticle::handleVCSList)

            vertx.createHttpServer()
                    .requestHandler({ router.accept(it) })
                    .listen(Config.Root.Port)
        }
    }

    fun handleIndex(ctx: RoutingContext) = with(ctx.response()) {
        putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.HTML)
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
        else ctx.response()
                .end(HttpResponseStatus.FORBIDDEN)
    }

    fun handleSettings(ctx: RoutingContext) {
        ctx.response().end(Config)
    }

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
        ctx.response().end(result)
    }

    fun handleDebugDatabaseCreate(ctx: RoutingContext) {
        val ds = vertx.getSharedDataSource()

        val q = jooq(ds).use {
            it.createTableIfNotExists("debug")
                    .column("id", PostgresDataType.SERIAL)
                    .column("payload", PostgresDataType.JSON)
                    .constraint(DSL.constraint("PK_DEBUG").primaryKey("id"))
                    .execute()
        }
        val ret = object : Jsonable {
            val query = q.toString()
        }
        ctx.jsonResponse().end(ret.toJson())
    }

    fun handleDebugCrash(ctx: RoutingContext) {
        launch(UnconfinedWithExceptions(ctx)) {
            vertx.delayAsync(500)
            throw IllegalStateException("Forced crash")
        }
    }

    fun handleDebugDatabaseFill(ctx: RoutingContext) {

        launch(UnconfinedWithExceptions(ctx)) {
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

    fun handleDebugDatabaseRead(ctx: RoutingContext) {

        launch(UnconfinedWithExceptions(ctx)) {
            val ds = vertx.getSharedDataSource()

            val id by ctx.request()

            val jsons =
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

            ctx.jsonResponse().end(Json.encode(jsons))
        }
    }

    fun handleDebugEventBus(ctx: RoutingContext) {
        launch(UnconfinedWithExceptions(ctx)) {
            val eb = vertx.eventBus()
            val req = ctx.request()
            val address by req
            address!!

            val body =
                    if (req.method() == HttpMethod.POST) {
                        req.bodyAsync().toJsonObject()
                    } else {
                        req.params()
                                .filterNot { (k, _) -> k == "address" }
                                .map { (k, v) -> Pair(k, JsonEx.decode(v)) }
                                .let { JsonObject(it.toMap(mutableMapOf<String, Any?>())) }
                    }
            val res = vxa<Message<Any>> { eb.send(address, body, it) }
            ctx.jsonResponse().end("${res.body()}")
        }
    }

    fun handleDebugDatabaseClear(ctx: RoutingContext) {

        launch(UnconfinedWithExceptions(ctx)) {
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
    }

    fun handleVCSPing(ctx: RoutingContext) {
        val req = ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val url by req
            val vcs by req

            val eb = vertx.eventBus()
            val message = object : Jsonable {
                val vcs = vcs
                val url = url.orEmpty().unquote()
            }
            val res = eb.sendAsync(Address.Code.Ping, message.toJson())

            ctx.response().end(res.body())
        }
    }

    fun handleVCSClone(ctx: RoutingContext) {
        val req = ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val url by req
            val vcs by req

            val eb = vertx.eventBus()
            val message = object : Jsonable {
                val vcs = vcs
                val url = url.orEmpty().unquote()
            }
            val res = eb.sendAsync(Address.Code.Download, message.toJson())

            ctx.response().end(res.body())
        }


    }

    fun handleVCSRead(ctx: RoutingContext) {
        val req = ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val param0 by req
            val uid = param0
            val param1 by req
            val path = param1
            val revision by req

            val eb = vertx.eventBus()
            val message = object : Jsonable {
                val uid = uid
                val path = path
                val revision = revision
            }
            val res = eb.sendAsync(Address.Code.Read, message.toJson())

            if (res.body().getBoolean("success")) ctx.jsonResponse().end(res.body())
            else ctx.response().setStatus(HttpResponseStatus.NOT_FOUND)
                    .end(HttpResponseStatus.NOT_FOUND.toJson().mergeIn(res.body()))
        }
    }

    fun handleVCSDiff(ctx: RoutingContext) {
        val req = ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val uid by req
            val path by req
            val from by req
            val to by req

            val eb = vertx.eventBus()
            val message = object : Jsonable {
                val uid = uid
                val path = path
                val from = from
                val to = to
            }
            val res = eb.sendAsync(Address.Code.Diff, message.toJson())

            if (res.body().getBoolean("success")) ctx.jsonResponse().end(res.body())
            else ctx.response().setStatus(HttpResponseStatus.NOT_FOUND)
                    .end(HttpResponseStatus.NOT_FOUND.toJson().mergeIn(res.body()))
        }
    }

    fun handleVCSList(ctx: RoutingContext) {
        val req = ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val uid by req
            val revision by req

            val eb = vertx.eventBus()
            val message = object : Jsonable {
                val uid = uid
                val revision = revision
            }
            val res = eb.sendAsync(Address.Code.List, message.toJson())

            if (res.body().getBoolean("success")) ctx.jsonResponse().end(res.body())
            else ctx.response().setStatus(HttpResponseStatus.NOT_FOUND)
                    .end(HttpResponseStatus.NOT_FOUND.toJson().mergeIn(res.body()))
        }
    }

    fun handleGsmsCreate(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        val key by ctx.request()
        val value by ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            vxu { gsms.put(key, value, it) }
            ctx.jsonResponse()
                    .end(
                            JsonObject(
                                    "vcs" to "create",
                                    "key" to key,
                                    "value" to value
                            )
                    )
        }
    }

    fun handleGsmsRead(ctx: RoutingContext) {
        val gsms = ctx.get<GSMS_TYPE>(GSMS_ID)

        val key by ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val value = vxa<String> { gsms.get(key, it) }
            ctx.jsonResponse()
                    .end(
                            JsonObject(
                                    "vcs" to "read",
                                    "key" to key,
                                    "value" to value
                            )
                    )
        }
    }

    fun handleTeamcity(ctx: RoutingContext) {
        val eb = vertx.eventBus()

        val address by ctx.request()

        launch(UnconfinedWithExceptions(ctx)) {
            val body = if (ctx.request().method() == HttpMethod.POST) {
                ctx.request().bodyAsync().toJsonObject()
            } else {
                JsonObject()
            }
            val res = eb.sendAsync<JsonObject>(
                    address.orEmpty(),
                    body
            )
            ctx.jsonResponse()
                    .end(res.body())
        }
    }

    fun handleFailure(ctx: RoutingContext) {
        val ex = ctx.failure()
        log.error("", ex)
        ctx.jsonResponse()
                .setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                .end(
                        JsonObject(
                                "result" to "failed",
                                "error" to ex.message
                        )
                )
    }
}
