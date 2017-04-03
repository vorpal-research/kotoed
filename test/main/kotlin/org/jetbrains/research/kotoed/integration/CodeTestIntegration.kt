package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.DefaultClientConfig
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.get
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder
import kotlin.test.assertEquals

class DebugEndpointTestIntegration: Loggable {

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

    fun wget(path: String, mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE): String {
        val config = DefaultClientConfig()
        val client = Client.create(config)
        val resource = client.resource(UriBuilder.fromUri("http://localhost:${Config.Root.Port}").build())

        return resource.path(path).accept(mediaType).get(String::class.java)
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
        val res = io.vertx.core.json.JsonArray(wget("debug/database/fill"))
        log.info(res.encodePrettily())
        assertEquals(3, res.size())

        assertEquals(Json.encode((res[0] as JsonObject)["payload"]),
                wget("debug/database/read/${(res[0] as JsonObject)["id"]}"))
        assertEquals(Json.encode((res[1] as JsonObject)["payload"]),
                wget("debug/database/read/${(res[1] as JsonObject)["id"]}"))
        assertEquals(Json.encode((res[2] as JsonObject)["payload"]),
                wget("debug/database/read/${(res[2] as JsonObject)["id"]}"))

        wget("debug/database/clear")
    }


}