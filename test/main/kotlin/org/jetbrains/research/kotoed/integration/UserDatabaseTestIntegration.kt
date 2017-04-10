package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.DefaultClientConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder
import kotlin.test.assertEquals

class UserDatabaseTestIntegration : Loggable {

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


    fun makeStupidUser(login: String, password: String = "password", salt: String = "sugar") =
        wpost("debug/eventbus/${Address.DB.create("denizen")}",
                payload = JsonObject("denizenid" to login, "password" to password, "salt" to salt).encodePrettily())

    fun makeStupidCourse(name: String, buildTemplateId: String = "") =
            wpost("debug/eventbus/${Address.DB.create("course")}",
                    payload = JsonObject("name" to name, "buildtemplateid" to buildTemplateId).encodePrettily())

    fun makeStupidProject(denizenId: Int, courseId: Int, repo: String = "git", repourl: String = "http://localhost") =
            wpost("debug/eventbus/${Address.DB.create("project")}",
                    payload = JsonObject(
                            "denizenid" to denizenId,
                            "courseid" to courseId,
                            "repotype" to repo,
                            "repourl" to repourl
                    ).encodePrettily())

    val JsonObject.id get() = getInteger("id")

    @Test
    fun basic() {

        val course = makeStupidCourse("Transmogrification 101").let(::JsonObject)

        // let's make sum users, shall we?

        val petya = makeStupidUser("Petya").let(::JsonObject)
        val vasya = makeStupidUser("Vasya").let(::JsonObject)
        val masha = makeStupidUser("Masha").let(::JsonObject)

        val petyasProject1 = makeStupidProject(petya.id, course.id).let(::JsonObject)
        val petyasProject2 = makeStupidProject(petya.id, course.id).let(::JsonObject)

        val refs = wget("debug/eventbus/${Address.DB.read("project")}.for.denizen",
                params = listOf("denizenid" to petya.id)).let(::JsonArray)

        assertEquals(setOf(petyasProject1, petyasProject2), refs.toSet())

    }
}

