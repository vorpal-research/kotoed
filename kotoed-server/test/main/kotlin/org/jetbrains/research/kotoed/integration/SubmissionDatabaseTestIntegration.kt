package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AnyAsJson
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

inline fun whileEx(condition: () -> Boolean, maxTries: Int = Int.MAX_VALUE, body: () -> Unit) {
    var tries = 0
    while (condition() && tries < maxTries) {
        ++tries; body(); }
}

class SubmissionDatabaseTestIntegration : Loggable {

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

    fun dbPost(address: String, payload: Jsonable) =
            wpost("debug/eventbus/$address", payload = payload.toJson().encode())
                    .let(::JsonObject)

    fun dbPostMany(address: String, payload: Jsonable) =
            wpost("debug/eventbus/$address", payload = payload.toJson().encode())
                    .let(::JsonArray)

    fun makeDbNew(entity: String, payload: Jsonable) =
            dbPost(Address.Api.create(entity), payload).getJsonObject("record")

    val JsonObject.id: Int? get() = getInteger("id")

    @Test
    fun testSimple() = with(AnyAsJson) {
        val user = makeDbNew(
            "denizen",
            object: Jsonable {
                val denizenId = "Vasyatka"
                val password = ""
                val salt = ""
            }
        )

        val course = makeDbNew(
            "course",
            object: Jsonable {
                val name = "KotlinAsFirst-2017"
            }
        )

        val project = makeDbNew(
            "project",
            object : Jsonable {
                val name = "Da_supa_mega_project"
                val denizen_id = user.id
                val course_id = course.id
                val repo_type = "git"
                val repo_url = "https://github.com/Kotlin-Polytech/KotlinAsFirst2016"
            }
        )


        var submission = makeDbNew(
            "submission",
            object : Jsonable {
                val projectId = project.id
                val revision = "153653"
            }
        )

        assert(submission.id is Int)

        whileEx({ submission.getString("state") != "open" }, maxTries = 200) {
            submission = dbPost(
                Address.Api.Submission.Read,
                object : Jsonable {
                    val id = submission.id
                }
            ).getJsonObject("record")
            Thread.sleep(100)
        }

        var resubmission = dbPost(
                Address.Api.Submission.Create,
                object : Jsonable {
                    val parent_submission_id = submission.id
                    val project_id = project.id
                    val revision = "fa8175"
                }
        ).getJsonObject("record")

        whileEx({ resubmission.getString("state") != "open" }, maxTries = 200) {
            resubmission = dbPost(
                    Address.Api.Submission.Read,
                    object : Jsonable {
                        val id = resubmission.id
                    }
            ).getJsonObject("record")
            Thread.sleep(100)
        }

        val comment = dbPost(
                Address.Api.Submission.Comment.Create,
                object : Jsonable {
                    val submission_id = submission.id
                    val sourcefile = "pom.xml"
                    val sourceline = 2
                    val text = "tl;dr"
                    val author_id = user.id
                }
        ).getJsonObject("record")

        println(comment)

        assertEquals(resubmission.id, comment.getInteger("submission_id"))

        var resubmission2 = dbPost(
                Address.Api.Submission.Create,
                object : Jsonable {
                    val parent_submission_id = resubmission.id
                    val project_id = project.id
                    val revision = "26cbba"
                }
        ).getJsonObject("record")

        whileEx({ resubmission2.getString("state") != "open" }, maxTries = 200) {
            resubmission2 = dbPost(
                    Address.Api.Submission.Read,
                    object : Jsonable {
                        val id = resubmission2.id
                    }
            ).getJsonObject("record")
            Thread.sleep(100)
        }

        val comments = dbPostMany(
                Address.Api.Submission.Comments,
                object : Jsonable {
                    val id = resubmission2.id
                }
        )

        assertEquals(1, comments.size())
        assertEquals("pom.xml", comments[0]["filename"])

        val byLine = comments[0]["by_line"] as? JsonArray
        assertEquals(1, byLine?.size())

        val firstByLine = byLine?.get(0)
        assertEquals(2, firstByLine["line"])

        val fblComments = firstByLine?.get("comments") as? JsonArray
        assertEquals(1, fblComments?.size())

        val theComment = fblComments[0] as JsonObject

        assertEquals(comment.getString("text"), theComment.getString("text"))
    }

}
