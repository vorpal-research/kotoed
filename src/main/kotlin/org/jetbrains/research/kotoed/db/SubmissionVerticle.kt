package org.jetbrains.research.kotoed.db

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
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
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

    // FIXME: insert teamcity calls to build the submission

    @JvmName("persistExt")
    protected suspend inline fun <reified R : UpdatableRecord<R>> R.persist(): R =
            persist(this, klassOf())

    @JvmName("persistAsCopyExt")
    protected suspend inline fun <reified R : UpdatableRecord<R>> R.persistAsCopy(): R =
            persistAsCopy(this, klassOf())

    @JvmName("selectByIdWhatever")
    protected suspend inline fun <reified R : UpdatableRecord<R>> selectById(instance: Table<R>, id: Int): R =
            selectById(instance, id, klassOf())

    private val SubmissionCommentRecord.location
        get() = Location(Filename(path = sourcefile), sourceline)

    private suspend fun recreateCommentsAsync(vcsUid: String, parent: SubmissionRecord, child: SubmissionRecord) {
        val eb = vertx.eventBus()
        eb.sendAsync<JsonArray>(
                Address.DB.readFor(Tables.SUBMISSION_COMMENT.name, Tables.SUBMISSION.name),
                JsonObject(Tables.SUBMISSION_COMMENT.SUBMISSION_ID.name to parent.id)
        )
                .body()
                .asSequence()
                .filterIsInstance<JsonObject>()
                .map { it.toRecord<SubmissionCommentRecord>() }
                .forEach { comment ->
                    comment.submissionId = child.id
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
                    JsonObject(Tables.SUBMISSION.PARENT_SUBMISSION_ID.name to submission.id)
            ).body().firstOrNull()?.run { cast<JsonObject>().toRecord() }

    @JsonableEventBusConsumerFor(Address.Submission.Read)
    suspend fun handleSubmissionRead(message: JsonObject): SubmissionRecord {
        val id: Int by message.delegate
        var submission: SubmissionRecord = selectById(Tables.SUBMISSION, id)
        val project: ProjectRecord = selectById(Tables.PROJECT, submission.projectId)

        val vcsReq: RepositoryInfo =
                sendJsonableAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
                )

        if (submission.state == Submissionstate.pending
                && vcsReq.status != CloneStatus.pending) {
            if (vcsReq.status != CloneStatus.failed) {
                submission.state = Submissionstate.open
                val parent: SubmissionRecord? = submission.parentSubmissionId?.let {
                    selectById(Tables.SUBMISSION, it)
                }

                if (parent != null) {
                    recreateCommentsAsync(vcsReq.uid, parent, submission)
                    parent.state = Submissionstate.obsolete
                    parent.persist<SubmissionRecord>()
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
            launch(UnconfinedWithExceptions { log.error("", it) }) {
                val (_, project, _) = run(UnconfinedWithExceptions(message)) {
                    var record: SubmissionRecord = message.body().toRecord()
                    // Set record's state to be pending
                    record.state = Submissionstate.pending

                    val project = selectById(Tables.PROJECT, record.projectId)
                    expect(project["id"] == record.projectId, "Illegal projectId")

                    val parent = record.parentSubmissionId?.let { selectById(Tables.SUBMISSION, it) }
                    expect(parent?.getValue("id") == record.parentSubmissionId, "Illegal parentsubmissionid")

                    parent?.apply {
                        expect(projectId == project.id)
                        expect(state != Submissionstate.obsolete)
                    }

                    record = record.persistAsCopy()
                    expect(record.id is Int)

                    message.reply(record.toJson())

                    Triple(record, project, parent)
                }

                val vcs = project.repoType
                val repository = project.repoUrl

                sendJsonableAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository))

            }

    @JsonableEventBusConsumerFor(Address.Submission.Comment.Create)
    suspend fun handleCommentCreate(message: SubmissionCommentRecord): SubmissionCommentRecord = run {
        val submission = selectById(Tables.SUBMISSION, message.submissionId)
        when (submission.state) {
            Submissionstate.open -> message.persistAsCopy()
            Submissionstate.obsolete -> {
                log.warn("Comment request for an obsolete submission received:" +
                        "Submission id = ${submission.id}")
                val parentSubmission = expectNotNull(findSuccessorAsync(submission))
                log.trace("Newer submission found: id = ${parentSubmission.id}")
                message.submissionId = parentSubmission.id
                // FIXME re-send not re-curse
                handleCommentCreate(message)
            }
            else -> throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
        }
    }

    @JsonableEventBusConsumerFor(Address.Submission.Comment.Read)
    suspend fun handleCommentRead(message: JsonObject): SubmissionCommentRecord =
            selectById(Tables.SUBMISSION_COMMENT, message.getInteger("id"))

    @JsonableEventBusConsumerFor(Address.Submission.Comments)
    suspend fun handleComments(message: SubmissionRecord): JsonObject {
        val arr = vertx.eventBus().sendAsync<JsonArray>(
                Address.DB.find(Tables.SUBMISSION_COMMENT.name),
                object : Jsonable {
                    val submission_id = message.id
                }.toJson()
        ).body()

        return object : Jsonable {
            val comments = arr
        }.toJson()
    }

}
