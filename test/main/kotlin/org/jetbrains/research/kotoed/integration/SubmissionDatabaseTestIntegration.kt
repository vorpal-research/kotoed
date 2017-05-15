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

class SubmissionDatabaseTestIntegration : Loggable {

    companion object {
        lateinit var server: Future<Vertx>
        @JvmStatic
        @BeforeClass
        fun before() {
            server = startServer()
            server.get()

            try {
                setupTC()
            } catch (ex: Exception) {}

        }

        @JvmStatic
        @AfterClass
        fun after() {
            stopServer(server)
        }
    }

    fun dbPost(address: String, payload: JsonObject) =
            wpost("debug/eventbus/$address", payload = payload.encode())
                    .let(::JsonObject)

    fun dbPostMany(address: String, payload: JsonObject) =
            wpost("debug/eventbus/$address", payload = payload.encode())
                    .let(::JsonArray)

    fun makeDbNew(entity: String, payload: JsonObject) =
            dbPost(Address.Api.create(entity), payload)

    val JsonObject.id: Int? get() = getInteger("id")

    @Test
    fun testSimple() = with(AnyAsJson) {
        val user = makeDbNew("denizen", JsonObject("""{ "denizen_id": "Vasyatka", "password" : "", "salt" : "" }""")).getJsonObject("record")
        val course = makeDbNew("course",
                JsonObject("""{ "name": "Transmogrification 101", "build_template_id" : "Test_build_template_id", "root_project_id" : "_Root" }""")).getJsonObject("record")
        val project = makeDbNew("project",
                object : Jsonable {
                    val name = "Da_supa_mega_project"
                    val denizen_id = user.id
                    val course_id = course.id
                    val repo_type = "mercurial"
                    val repo_url = "http://bitbucket.org/vorpal-research/kotoed"
                }.toJson()).getJsonObject("record")


        var submission = dbPost(
                Address.Api.Submission.Create,
                object : Jsonable {
                    val project_id = project.id
                    val revision = "1942a948d720fb786fc8c2e58af335eea2e2fe90"
                }.toJson()).getJsonObject("record")

        assert(submission.id is Int)

        while (submission.getString("state") != "open") {
            submission = dbPost(
                    Address.Api.Submission.Read,
                    object : Jsonable {
                        val id = submission.id
                    }.toJson()
            ).getJsonObject("record")
            Thread.sleep(100)
        }

        var resubmission = dbPost(
                Address.Api.Submission.Create,
                object : Jsonable {
                    val parent_submission_id = submission.id
                    val project_id = project.id
                    val revision = "82b75aa179ef4d20b2870df88c37657ecb2b9f6b"
                }.toJson()).getJsonObject("record")

        while (resubmission.getString("state") != "open") {
            resubmission = dbPost(
                    Address.Api.Submission.Read,
                    object : Jsonable {
                        val id = resubmission.id
                    }.toJson()
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
                }.toJson())

        assertEquals(resubmission.id, comment.getInteger("submission_id"))

        var resubmission2 = dbPost(
                Address.Api.Submission.Create,
                object : Jsonable {
                    val parent_submission_id = resubmission.id
                    val project_id = project.id
                    val revision = "9fc0841dcdfaf274fc9b71a790dd6a46d21731d8"
                }.toJson()).getJsonObject("record")

        while (resubmission2.getString("state") != "open") {
            resubmission2 = dbPost(
                    Address.Api.Submission.Read,
                    object : Jsonable {
                        val id = resubmission2.id
                    }.toJson()
            ).getJsonObject("record")
            Thread.sleep(100)
        }

        val comments = dbPostMany(
                Address.Api.Submission.Comments,
                object : Jsonable {
                    val id = resubmission2.id
                }.toJson())

        assertEquals(1, comments.size())
        assertEquals(comment.getString("text"), comments.getJsonObject(0).getString("text"))
    }

}
