package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.eventbus.Address
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

    fun makeDbNew(entity: String, payload: JsonObject) =
            dbPost(Address.DB.create(entity), payload)

    val JsonObject.id get() = getInteger("id")

    @Test
    fun testSimple() {
        val user = makeDbNew("denizen", JsonObject("""{ "denizenid": "Vasyatka", "password" : "", "salt" : "" }"""))
        val course = makeDbNew("course", JsonObject("""{ "name": "Transmogrification 101", "buildtemplateid" : "" }"""))
        val project = makeDbNew("project",
                object: Jsonable {
                    val denizenid = user.id
                    val courseid = course.id
                    val repotype = "mercurial"
                    val repourl = "http://bitbucket.org/vorpal-research/kotoed"
                }.toJson())


        var submission = dbPost(
                Address.Submission.Create,
                object: Jsonable {
                    val projectid = project.id
                    val revision = "1942a948d720fb786fc8c2e58af335eea2e2fe90"
                }.toJson())

        assert(submission.id is Int)

        while(submission.getString("state") != "open") {
            submission = dbPost(
                    Address.Submission.Read,
                    object: Jsonable {
                        val id = submission.id
                    }.toJson()
            )
            Thread.sleep(100)
        }

        var resubmission = dbPost(
                Address.Submission.Create,
                object: Jsonable {
                    val parentsubmissionid = submission.id
                    val projectid = project.id
                    val revision = "82b75aa179ef4d20b2870df88c37657ecb2b9f6b"
                }.toJson())

        while(resubmission.getString("state") != "open") {
            resubmission = dbPost(
                    Address.Submission.Read,
                    object: Jsonable {
                        val id = resubmission.id
                    }.toJson()
            )
            Thread.sleep(100)
        }

        val comment = dbPost(
                Address.Submission.Comment.Create,
                object: Jsonable {
                    val submissionid = submission.id
                    val sourcefile = "pom.xml"
                    val sourceline = 2
                    val text = "tl;dr"
                }.toJson())

        assertEquals(resubmission.id, comment.getInteger("submissionid"))

        var resubmission2 = dbPost(
                Address.Submission.Create,
                object: Jsonable {
                    val parentsubmissionid = resubmission.id
                    val projectid = project.id
                    val revision = "9fc0841dcdfaf274fc9b71a790dd6a46d21731d8"
                }.toJson())

        while(resubmission2.getString("state") != "open") {
            resubmission2 = dbPost(
                    Address.Submission.Read,
                    object: Jsonable {
                        val id = resubmission2.id
                    }.toJson()
            )
            Thread.sleep(100)
        }

        val comments = dbPost(
                Address.Submission.Comments,
                object: Jsonable {
                    val id = resubmission2.id
                }.toJson()).getJsonArray("comments")

        assertEquals(1, comments.size())
        assertEquals(comment.getString("text"), comments.getJsonObject(0).getString("text"))
    }

}