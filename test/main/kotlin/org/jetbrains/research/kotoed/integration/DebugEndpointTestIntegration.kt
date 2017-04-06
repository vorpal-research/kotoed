package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.DefaultClientConfig
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugEndpointTestIntegration : Loggable {

    companion object {
        lateinit var server: Future<Vertx>
        @JvmStatic
        @BeforeClass
        fun before() {
            server = startServer()
            server.get()
        }

        @JvmStatic
        @AfterClass
        fun after() {
            stopServer(server)
        }
    }

    fun wget(path: String, mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
             params: Iterable<Pair<String, Any?>> = listOf()): String {
        val config = DefaultClientConfig()
        val client = Client.create(config)
        val resource = client.resource(UriBuilder.fromUri("http://localhost:${Config.Root.Port}").build())

        return resource.path(path).let {
            params.fold(it) { res, (k, v) -> res.queryParam(k, "$v") }
        }.accept(mediaType).get(String::class.java)
    }

    fun wpost(path: String, mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE, payload: String = ""): String {
        val config = DefaultClientConfig()
        val client = Client.create(config)
        val resource = client.resource(UriBuilder.fromUri("http://localhost:${Config.Root.Port}").build())

        return resource.path(path).accept(mediaType).post(String::class.java, payload)
    }

    @Test
    fun allohaWorld() {
        log.info("Hi, I am a server-aware test!")
    }

    @Test
    fun index() {
        val contents = wget("")

        log.info("Index page: $contents")
    }

    @Test
    fun debugDb() {
        wget("debug/database/clear")
        val res = JsonArray(wget("debug/database/fill"))
        log.info(res.encodePrettily())

        assertEquals(3, res.size())

        with(AnyAsJson) {
            assertEquals(Json.encode(res[0]["payload"]),
                    wget("debug/database/read/${res[0]["id"]}"))
            assertEquals(Json.encode(res[1]["payload"]),
                    wget("debug/database/read/${res[1]["id"]}"))
            assertEquals(Json.encode(res[2]["payload"]),
                    wget("debug/database/read/${res[2]["id"]}"))
        }

        wget("debug/database/clear")
    }

    @Test
    fun debugDb2() {
        val data = listOf(2, "Hello", JsonArray((0..3).toList()), JsonObject(), JsonObject("value" to null))

        val ids = data.map { datum ->
            val res = wpost(
                    "debug/eventbus/${Address.DB.create("debug")}",
                    payload = JsonObject("payload" to datum).encodePrettily()
            )
            log.info(res)
            JsonObject(res)["id"]
        }

        with(AnyAsJson) {
            for (i in 0..data.size - 1) {
                val id = ids[i]
                val lhv = JsonObject(
                        wget(
                                "debug/eventbus/${Address.DB.read("debug")}",
                                params = listOf("id" to id)
                        )
                )["payload"]
                val rhv = data[i]
                // not using assertEquals here, because ordering is important!
                assertTrue(
                        lhv == rhv, "$lhv != $rhv"
                )
            }
        }

        for (id in ids) {
            wget("debug/eventbus/${Address.DB.delete("debug")}", params = listOf("id" to id))
        }

        for (id in ids) {
            assertEquals("null", wget("debug/eventbus/${Address.DB.read("debug")}", params = listOf("id" to id)))
            assertEquals("null", wget("debug/eventbus/${Address.DB.delete("debug")}", params = listOf("id" to id)))
        }
    }

}