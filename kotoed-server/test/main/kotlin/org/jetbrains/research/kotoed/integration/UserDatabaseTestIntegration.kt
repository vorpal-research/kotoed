package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
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

    fun makeStupidUser(login: String, password: String = "password", salt: String = "sugar") =
            wpost("debug/eventbus/${Address.DB.create("denizen")}",
                    payload = JsonObject("denizen_id" to login, "password" to password, "salt" to salt).encodePrettily())

    fun makeStupidCourse(name: String, buildTemplateId: String = "") =
            wpost("debug/eventbus/${Address.DB.create("course")}",
                    payload = JsonObject("name" to name, "build_template_id" to buildTemplateId).encodePrettily())

    fun makeStupidProject(denizenId: Int, courseId: Int, repo: String = "git", repourl: String = "http://localhost") =
            wpost("debug/eventbus/${Address.DB.create("project")}",
                    payload = JsonObject(
                            "denizen_id" to denizenId,
                            "course_id" to courseId,
                            "repo_type" to repo,
                            "repo_url" to repourl
                    ).encodePrettily())

    val JsonObject.id: Int get() = getInteger("id")

    @Test
    fun basic() {

        val course = makeStupidCourse("Transmogrification 102").let(::JsonObject)

        // let's make sum users, shall we?

        val petya = makeStupidUser("Petya").let(::JsonObject)
        val vasya = makeStupidUser("Vasya").let(::JsonObject)
        val masha = makeStupidUser("Masha").let(::JsonObject)

        val petyasProject1 = makeStupidProject(petya.id, course.id).let(::JsonObject)
        val petyasProject2 = makeStupidProject(petya.id, course.id).let(::JsonObject)

        val refs = wget("debug/eventbus/${Address.DB.read("project")}.for.denizen",
                params = listOf("denizen_id" to petya.id)).let(::JsonArray)

        assertEquals(setOf(petyasProject1, petyasProject2), refs.toSet())

    }
}
