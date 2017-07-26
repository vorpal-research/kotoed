package org.jetbrains.research.kotoed.routers

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsonResponse
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
@JsonResponse
fun RoutingContext.handleDebugSettings() = response().end(Config)

@HandlerFor("/debug/request")
@JsonResponse
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
    response().end(result)
}

@HandlerFor("/debug/crash/now")
@JsonResponse
fun RoutingContext.handleDebugCrashNow(): Unit = throw IllegalStateException("Forced crash")

@HandlerFor("/debug/crash/suspend")
@JsonResponse
suspend fun RoutingContext.handleDebugCrashSuspend(): Unit = throw IllegalStateException("Forced crash")

@HandlerFor("/debug/crash/delay")
@JsonResponse
suspend fun RoutingContext.handleDebugCrash() {
    val vertx = vertx()
    vertx.delayAsync(500)
    handleDebugCrashNow()
}

@HandlerFor("/debug/database/create")
@JsonResponse
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
    response().end(res)
}

@HandlerFor("/debug/database/fill")
@JsonResponse
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

    response().end(JsonArray(res).encodePrettily())
}

@HandlerFor("/debug/database/read/:id")
@JsonResponse
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

    response().end(Json.encode(res))
}

@HandlerFor("/debug/database/clear")
@JsonResponse
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

    response().end(JsonObject("success" to true))
}

@HandlerFor("/debug/eventbus/:address")
@JsonResponse
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
    response().end("${res.body()}")
}

internal fun cssClassByPath(path: String) =
        path.split(".").last().let {
            when(it){
                "java" -> "language-java"
                "kt" -> "language-kotlin"
                "xml", "iml" -> "language-xml"
                "sql" -> "language-sql"
                "json" -> "language-json"
                "yml", "yaml" -> "language-yaml"
                else -> "language-$it"
            }
        }

@HandlerFor("""\/debug\/code\/([^\/]+)\/([^\/]+)\/(.*)""", isRegex = true)
suspend fun RoutingContext.handleDebugCode() = with(response()) {
    val req = request()
    val param0 by req
    var uid = param0
    val param1 by req
    val revision = param1
    val param2 by req
    val path = param2
    val eb = vertx().eventBus()

    if(uid == "byUrl") {
        val url by req
        val message = object : Jsonable {
            val url = url.orEmpty().unquote()
        }

        val res = eb.sendAsync(Address.Code.Download, message.toJson()).body()

        println("res = ${res.encodePrettily()}")

        uid = res.getString("uid")

        when(res.getString("status")) {
            "pending" -> throw KotoedException(202, "Result not ready yet")
            "failed" -> throw KotoedException(404, "Repository not found")
            else -> {}
        }
    }

    val message = object : Jsonable {
        val uid = uid
        val path = path
        val revision = revision
    }

    val res =
            when{
                path!!.isEmpty() || path.endsWith("/") -> eb.sendAsync(Address.Code.List, message.toJson()).body()
                else -> eb.sendAsync(Address.Code.Read, message.toJson()).body()
            }

    val supportedLanguages = setOf(
            "java", "kotlin", "xml", "yaml", "json",
            "javascript", "markdown", "sql", "asciidoc", "bash"
    )

    fun MetaDataContent.css(href: String) = link(href = href, rel = "stylesheet")
    fun FlowContent.bsContainer(block: DIV.() -> Unit = {}) = div(classes = "container", block = block)
    fun FlowContent.bsListGroup(block: DIV.() -> Unit = {}) = div(classes = "list-group", block = block)
    fun FlowContent.bsListGroupItem(href: String? = null, block: A.() -> Unit = {}) =
            a(classes = "list-group-item", href = href, block = block)

    putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValuesEx.HTML)
            .end(createHTML().html {
                head {
                    title("The awesome kotoed")
                    meta { charset = "UTF-8" }
                    meta { name = "viewport"; content = "width=device-width, initial-scale=1" }
                    css("https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/themes/prism.css")
                    css("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
                    script { src = "https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js" }
                    script { src = "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" }
                    script { src = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/prism.min.js" }
                    for(lang in supportedLanguages) {
                        script{ src = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/components/prism-$lang.min.js" }
                    }
                }
                body {
                    if(res.containsKey("contents")) {
                        bsContainer {
                            bsListGroup {
                                if (path.isNotEmpty())
                                    bsListGroupItem(href = "/debug/code/$uid/$revision/${path}/..") { +"[up]" }
                            }
                            pre { code(classes = path?.let(::cssClassByPath)) { +res.getValue("contents").toString() }  }
                        }
                    } else if(res.containsKey("files")) {
                        val prep = res
                                .getJsonArray("files")
                                .map { it.toString() }
                                .filter { it.startsWith(path) }
                                .map { it.removePrefix(path).split("/") }
                                .map { it.first() + if(it.size > 1) "/" else "" }
                                .toSet()

                        bsContainer {
                            if (path.isNotEmpty())
                                bsListGroup {
                                    bsListGroupItem(href = "/debug/code/$uid/$revision/${path}..") { +"[up]" }
                                }
                            bsListGroup {
                                for (f in prep) {
                                    bsListGroupItem(href = "/debug/code/$uid/$revision/${path + f}") { +f }
                                }
                            }
                        }
                    }
                }
            })
}
