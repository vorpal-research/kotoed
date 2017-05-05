package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.UniformInterfaceException
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AnyAsJson
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.get
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun alohaWorld() {
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
            assertEquals(
                    400,
                    assertFailsWith(UniformInterfaceException::class) {
                        wget("debug/eventbus/${Address.DB.read("debug")}", params = listOf("id" to id)).let(::JsonObject)
                    }.response.status
            )
            assertEquals(
                    400,
                    assertFailsWith(UniformInterfaceException::class) {
                        wget("debug/eventbus/${Address.DB.delete("debug")}", params = listOf("id" to id)).let(::JsonObject)
                    }.response.status
            )
        }
    }

}
