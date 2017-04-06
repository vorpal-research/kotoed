package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.data.vcs.RemoteRequest
import org.jetbrains.research.kotoed.data.vcs.VCS
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson

class SubmissionDatabaseVerticle: DatabaseVerticle<SubmissionRecord>(Tables.SUBMISSION) {
    override fun handleDelete(message: Message<JsonObject>)  =
            launch(UnconfinedWithExceptions(message)){
                throw IllegalArgumentException("Submissions are not deletable")
            }.ignore()
}

class SubmissionVerticle: AbstractVerticle() {
    override fun start() {
        vertx.deployVerticle(SubmissionDatabaseVerticle())
    }

    internal fun idQuery(id: Int) = JsonObject("id" to id)

    fun handleSubmissionCreate(message: Message<JsonObject>) =
        launch(UnconfinedWithExceptions {}) {
            val eb = vertx.eventBus()
            val (sub, project, parent) = run(UnconfinedWithExceptions(message)){
                val record = SubmissionRecord().apply { from(message.body().map) }
                record.state = Submissionstate.pending

                val project = eb.sendAsync(Address.DB.read("project"), idQuery(record.projectid)).body()
                expect(project["id"] == record.projectid, "Illegal projectId")

                val projectRecord = ProjectRecord().apply { from(project) }

                val parent = record.parentsubmissionid?.let {
                    eb.sendAsync(Address.DB.read("submission"), idQuery(it)).body()
                }
                expect(parent?.getValue("id") == record.parentsubmissionid, "Illegal parentsubmissionid")
                val parentRecord = SubmissionRecord().apply { from(parent) }

                val ret = eb.sendAsync(Address.DB.create("submission"), record.toJson()).body()
                expect(ret["id"] is Int)

                record.apply { from(ret) }
                message.reply(ret)
                Triple(record, projectRecord, parentRecord)
            }

            val revision = sub.revision
            val vcs = project.repotype
            val repository = project.repourl

            eb.sendAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository).toJson())
        }.ignore()

}