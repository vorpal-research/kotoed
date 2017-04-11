package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.data.vcs.RemoteRequest
import org.jetbrains.research.kotoed.data.vcs.RepositoryInfo
import org.jetbrains.research.kotoed.data.vcs.VCS
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissioncommentRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

class SubmissionDatabaseVerticle : DatabaseVerticle<SubmissionRecord>(Tables.SUBMISSION) {
    override fun handleDelete(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions(message)) {
                throw IllegalArgumentException("Submissions are not deletable")
            }.ignore()
}

class SubmissionVerticle : AbstractVerticle() {
    override fun start() {
        vertx.deployVerticle(SubmissionDatabaseVerticle())
    }

    internal fun idQuery(id: Int) = JsonObject("id" to id)

    // FIXME: insert teamcity calls to build the submission

    fun handleSubmissionRead(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions(message)) {
                val eb = vertx.eventBus()

                val body = message.body()
                val vcs: String by body.delegate
                val repourl: String by body.delegate
                val path: String by body.delegate

                val vcsReq: RepositoryInfo =
                        eb.sendAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repourl).toJson())
                                .body()
                                .toJsonable()


                val id: Int by body.delegate
                var submission: SubmissionRecord = eb.sendAsync(Address.DB.read("project"), idQuery(id)).body().toRecord()

                if (submission.state == Submissionstate.pending
                        && vcsReq.status != CloneStatus.pending) {
                    if (vcsReq.success) {
                        submission.state = Submissionstate.open
                    } else {
                        submission.state = Submissionstate.invalid
                    }
                    submission = eb.sendAsync(Address.DB.update("project"), submission.toJson()).body().toRecord()
                }

                message.reply(submission)
            }.ignore()

    fun handleSubmissionCreate(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions {}) {
                val eb = vertx.eventBus()
                val (sub, project, parent) = run(UnconfinedWithExceptions(message)) {
                    val record: SubmissionRecord = message.body().toRecord()
                    record.state = Submissionstate.pending

                    val project = eb.sendAsync(Address.DB.read("project"), idQuery(record.projectid)).body()
                    expect(project["id"] == record.projectid, "Illegal projectId")

                    val projectRecord: ProjectRecord = project.toRecord()

                    val parent = record.parentsubmissionid?.let {
                        eb.sendAsync(Address.DB.read("submission"), idQuery(it)).body()
                    }
                    expect(parent?.getValue("id") == record.parentsubmissionid, "Illegal parentsubmissionid")
                    val parentRecord: SubmissionRecord? = parent?.toRecord()

                    val ret = eb.sendAsync(Address.DB.create("submission"), record.toJson()).body()
                    expect(ret["id"] is Int)

                    record.apply { from(ret) }
                    message.reply(ret)
                    Triple(record, projectRecord, parentRecord)
                }

                if (parent != null) {
                    eb.sendAsync<JsonArray>(
                            Address.DB.readFor("submissioncomment", "submission"),
                            JsonObject("submissionid" to parent.id)
                    )
                            .body()
                            .asSequence()
                            .filterIsInstance<JsonObject>()
                            .map { it.toRecord<SubmissioncommentRecord>() }
                            .forEach { comment ->
                                comment.submissionid = sub.id
                                eb.sendAsync(Address.DB.create("submissioncomment"), comment.toJson())
                            }
                }

                val revision = sub.revision
                val vcs = project.repotype
                val repository = project.repourl

                eb.sendAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository).toJson())
            }.ignore()

}