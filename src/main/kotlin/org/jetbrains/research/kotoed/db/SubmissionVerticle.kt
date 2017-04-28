package org.jetbrains.research.kotoed.db

import io.vertx.core.CompositeFuture
import io.vertx.core.Future
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
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissioncommentRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Table
import org.jooq.UpdatableRecord

@AutoDeployable
class SubmissionDatabaseVerticle : CrudDatabaseVerticleWithReferences<SubmissionRecord>(Tables.SUBMISSION) {
    override fun handleDelete(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions(message)) {
                throw IllegalArgumentException("Submissions are not deletable")
            }.ignore()
}

@AutoDeployable
class SubmissionVerticle : AbstractKotoedVerticle(), Loggable {
    override fun start(startFuture: Future<Void>) {
        val f = Future.future<String>()
        val ff = Future.future<Void>()

        vertx.deployVerticle(SubmissionDatabaseVerticle(), f)
        super.start(ff)

        CompositeFuture.all(f, ff).setHandler { startFuture.complete() }
    }

    // FIXME: insert teamcity calls to build the submission

    private val SubmissioncommentRecord.location
        get() = Location(Filename(path = sourcefile), sourceline)

    private inline suspend fun <reified R : UpdatableRecord<R>> R.persist(): R =
            sendJsonableAsync(Address.DB.update(table.name), this)

    private inline suspend fun <reified R : UpdatableRecord<R>> R.persistAsCopy(): R =
            sendJsonableAsync(Address.DB.create(table.name), this)

    private inline suspend fun <reified R : UpdatableRecord<R>> selectById(instance: Table<R>, id: Int): R =
            sendJsonableAsync(Address.DB.read(instance.name), JsonObject("id" to id))

    private suspend fun recreateCommentsAsync(vcsUid: String, parent: SubmissionRecord, child: SubmissionRecord) {
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
                    val adjustedLocation: LocationResponse =
                            sendJsonableAsync(
                                    Address.Code.LocationDiff,
                                    LocationRequest(
                                            vcsUid,
                                            comment.location,
                                            parent.revision,
                                            child.revision
                                    )
                            )
                    comment.sourcefile = adjustedLocation.location.filename.path
                    comment.sourceline = adjustedLocation.location.line
                    comment.id = null
                    comment.persistAsCopy()
                }

        parent.state = Submissionstate.obsolete

        parent.persist()
    }

    private suspend fun findSuccessorAsync(submission: SubmissionRecord): SubmissionRecord? =
            vertx.eventBus().sendAsync<JsonArray>(
                    Address.DB.find(Tables.SUBMISSION.name),
                    JsonObject("parentsubmissionid" to submission.id)
            ).body().firstOrNull()?.run { cast<JsonObject>().toRecord() }

    @JsonableEventBusConsumerFor(Address.Submission.Read)
    suspend fun handleSubmissionRead(message: JsonObject): SubmissionRecord {
        val id: Int by message.delegate
        var submission: SubmissionRecord = selectById(Tables.SUBMISSION, id)
        val project: ProjectRecord = selectById(Tables.PROJECT, submission.projectid)

        val vcsReq: RepositoryInfo =
                sendJsonableAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repotype), project.repourl).toJson()
                )

        if (submission.state == Submissionstate.pending
                && vcsReq.status != CloneStatus.pending) {
            if (vcsReq.status != CloneStatus.failed) {
                submission.state = Submissionstate.open
                val parent: SubmissionRecord? = submission.parentsubmissionid?.let {
                    selectById(Tables.SUBMISSION, it)
                }

                if (parent != null) {
                    recreateCommentsAsync(vcsReq.uid, parent, submission)
                    parent.state = Submissionstate.obsolete
                    parent.persist()
                }
            } else {
                submission.state = Submissionstate.invalid
            }
            submission = submission.persist()
        }

        return submission
    }

    @EventBusConsumerFor(Address.Submission.Create)
    fun handleSubmissionCreate(message: Message<JsonObject>) =
            launch(UnconfinedWithExceptions {}) {
                val (_, project, _) = run(UnconfinedWithExceptions(message)) {
                    var record: SubmissionRecord = message.body().toRecord()
                    record.state = Submissionstate.pending

                    val project = selectById(Tables.PROJECT, record.projectid)
                    expect(project["id"] == record.projectid, "Illegal projectId")

                    val parent = record.parentsubmissionid?.let { selectById(Tables.SUBMISSION, it) }
                    expect(parent?.getValue("id") == record.parentsubmissionid, "Illegal parentsubmissionid")

                    parent?.apply {
                        expect(projectid == project.id)
                        expect(state != Submissionstate.obsolete)
                    }

                    record = record.persistAsCopy()
                    expect(record.id is Int)

                    message.reply(record.toJson())
                    Triple(record, project, parent)
                }

                val vcs = project.repotype
                val repository = project.repourl

                sendJsonableAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository))

            }

    @JsonableEventBusConsumerFor(Address.Submission.Comment.Create)
    suspend fun handleCommentCreate(message: SubmissioncommentRecord): SubmissioncommentRecord = run {
        val submission = selectById(Tables.SUBMISSION, message.submissionid)
        when (submission.state) {
            Submissionstate.open -> message.persistAsCopy()
            Submissionstate.obsolete -> {
                log.warn("Comment request for an obsolete submission received:" +
                        "Submission id = ${submission.id}")
                val parentSubmission = expectNotNull(findSuccessorAsync(submission))
                log.trace("Newer submission found: id = ${parentSubmission.id}")
                message.submissionid = parentSubmission.id
                handleCommentCreate(message)
            }
            else -> throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
        }
    }

    @JsonableEventBusConsumerFor(Address.Submission.Comment.Read)
    suspend fun handleCommentRead(message: JsonObject): SubmissioncommentRecord =
            selectById(Tables.SUBMISSIONCOMMENT, message.getInteger("id"))

    @JsonableEventBusConsumerFor(Address.Submission.Comments)
    suspend fun handleComments(message: SubmissionRecord): JsonObject {
        val arr = vertx.eventBus().sendAsync<JsonArray>(Address.DB.find(Tables.SUBMISSIONCOMMENT.name),
                object : Jsonable {
                    val submissionid = message.id
                }.toJson()).body()

        return object : Jsonable {
            val comments = arr
        }.toJson()
    }

}