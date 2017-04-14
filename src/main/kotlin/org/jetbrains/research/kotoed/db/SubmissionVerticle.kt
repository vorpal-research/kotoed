package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.Submission
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissioncommentRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Table
import org.jooq.UpdatableRecord

class SubmissionDatabaseVerticle : DatabaseVerticle<SubmissionRecord>(Tables.SUBMISSION) {
    override fun handleDelete(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions(message)) {
                throw IllegalArgumentException("Submissions are not deletable")
            }.ignore()
}

class SubmissionVerticle : AbstractKotoedVerticle() {
    override fun start() {
        vertx.deployVerticle(SubmissionDatabaseVerticle())
        super.start()
    }

    internal fun idQuery(id: Int) = JsonObject("id" to id)

    // FIXME: insert teamcity calls to build the submission

    private val SubmissioncommentRecord.location
        get() = Location(Filename(path = sourcefile), sourceline)

    private inline suspend fun <reified R : UpdatableRecord<R>> R.persist(): R =
            vertx.eventBus().sendAsync(Address.DB.update(table.name), toJson()).body().toRecord()

    private inline suspend fun <reified R : UpdatableRecord<R>> R.persistAsCopy(): R =
            vertx.eventBus().sendAsync(Address.DB.create(table.name), toJson()).body().toRecord()

    private inline suspend fun <reified R : UpdatableRecord<R>> selectById(instance: Table<R>, id: Int): R =
            vertx.eventBus().sendAsync(Address.DB.read(instance.name), JsonObject("id" to id)).body().toRecord()

    private suspend fun recreateComments(vcsUid: String, parent: SubmissionRecord, child: SubmissionRecord) {
        val eb = vertx.eventBus()
        eb.sendAsync<JsonArray>(
                Address.DB.readFor("submissioncomment", "submission"),
                JsonObject("submissionid" to parent.id)
        )
                .body()
                .asSequence()
                .filterIsInstance<JsonObject>()
                .map { it.toRecord<SubmissioncommentRecord>() }
                .forEach { comment ->
                    comment.submissionid = child.id
                    val adjustedLocation: LocationResponse = eb.sendAsync(
                            Address.Code.LocationDiff,
                            LocationRequest(
                                    vcsUid,
                                    comment.location,
                                    parent.revision,
                                    child.revision
                            ).toJson()
                    ).body().toJsonable()
                    comment.sourcefile = adjustedLocation.location.filename.path
                    comment.sourceline = adjustedLocation.location.line
                    comment.persistAsCopy()
                }

        parent.state = Submissionstate.obsolete

        parent.persist()
    }

    @EventBusConsumerFor(Address.Submission.Read)
    suspend fun handleSubmissionRead(message: Message<JsonObject>) {
        val eb = vertx.eventBus()

        val id: Int by message.body().delegate
        var submission: SubmissionRecord = selectById(Tables.SUBMISSION, id)
        val project: ProjectRecord = selectById(Tables.PROJECT, submission.projectid)

        val vcsReq: RepositoryInfo =
                eb.sendAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repotype), project.repourl).toJson()
                )
                        .body()
                        .toJsonable()

        if (submission.state == Submissionstate.pending
                && vcsReq.status != CloneStatus.pending) {
            if (vcsReq.status != CloneStatus.failed) {
                submission.state = Submissionstate.open
                val parent: SubmissionRecord? = submission.parentsubmissionid?.let {
                    selectById(Tables.SUBMISSION, it)
                }

                if (parent != null) {
                    recreateComments(vcsReq.uid, parent, submission)
                    parent.state = Submissionstate.obsolete
                    parent.persist()
                }
            } else {
                submission.state = Submissionstate.invalid
            }
            submission = submission.persist()
        }

        message.reply(submission.toJson())
    }

    fun handleSubmissionCreate(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions {}) {
                val eb = vertx.eventBus()
                val (_, project, _) = run(UnconfinedWithExceptions(message)) {
                    val record: SubmissionRecord = message.body().toRecord()
                    record.state = Submissionstate.pending

                    val project = selectById(Tables.PROJECT, record.projectid)
                    expect(project["id"] == record.projectid, "Illegal projectId")

                    val parent = record.parentsubmissionid?.let { selectById(Tables.SUBMISSION, it) }
                    expect(parent?.getValue("id") == record.parentsubmissionid, "Illegal parentsubmissionid")

                    parent?.apply {
                        expect(projectid == project.id)
                        expect(state != Submissionstate.obsolete)
                    }

                    val ret = record.persistAsCopy()
                    expect(ret["id"] is Int)

                    record.apply { from(ret) }
                    message.reply(ret)
                    Triple(record, project, parent)
                }

                val vcs = project.repotype
                val repository = project.repourl

                eb.sendAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository).toJson())
            }.ignore()

}