package org.jetbrains.research.kotoed.api

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.Submissionstate
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Table
import org.jooq.UpdatableRecord

@AutoDeployable
class SubmissionVerticle : AbstractKotoedVerticle(), Loggable {

    // FIXME: insert teamcity calls to build the submission

    @JvmName("persistExt")
    protected suspend inline fun <reified R : UpdatableRecord<R>> R.persist(): R =
            dbUpdateAsync(this, klassOf())

    @JvmName("persistAsCopyExt")
    protected suspend inline fun <reified R : UpdatableRecord<R>> R.persistAsCopy(): R =
            dbCreateAsync(this, klassOf())

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
                    dbCreateAsync(comment)
                }

        parent.state = Submissionstate.obsolete

        dbUpdateAsync(parent)
    }

    private suspend fun findSuccessorAsync(submission: SubmissionRecord): SubmissionRecord? =
            dbFindAsync(SubmissionRecord().apply { parentSubmissionId = submission.id })
                    .apply { expect(size <= 1, "More than one parent found for submission $submission") }
                    .firstOrNull()

    @JsonableEventBusConsumerFor(Address.Api.Submission.Read)
    suspend fun handleSubmissionRead(sub: SubmissionRecord): SubmissionRecord {
        var submission = dbFetchAsync(sub)
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
                    dbUpdateAsync(parent)
                }
            } else {
                submission.state = Submissionstate.invalid
            }
            submission = dbUpdateAsync(submission)
        }

        return submission
    }

    @EventBusConsumerFor(Address.Api.Submission.Create)
    suspend fun handleSubmissionCreate(message: Message<JsonObject>) {
        var record: SubmissionRecord = message.body().toRecord()
        // Set record's state to be pending
        record.state = Submissionstate.pending

        val project = selectById(Tables.PROJECT, record.projectId)
        expect(project.id == record.projectId, "Illegal projectId")

        val parent = record.parentSubmissionId?.let { selectById(Tables.SUBMISSION, it) }
        expect(parent?.id == record.parentSubmissionId, "Illegal parentsubmissionid")

        parent?.apply {
            expect(projectId == project.id)
            expect(state != Submissionstate.obsolete)
        }

        record = dbCreateAsync(record)
        expect(record.id is Int)

        message.reply(record.toJson())

        val vcs = project.repoType
        val repository = project.repoUrl

        // return is here to force Unit as return type of the query
        return sendJsonableAsync(Address.Code.Download, RemoteRequest(VCS.valueOf(vcs), repository))
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Create)
    suspend fun handleCommentCreate(message: SubmissionCommentRecord): SubmissionCommentRecord = run {
        val submission = selectById(Tables.SUBMISSION, message.submissionId)
        when (submission.state) {
            Submissionstate.open -> message.persistAsCopy()
            Submissionstate.obsolete -> {
                log.warn("Comment request for an obsolete submission received:" +
                        "Submission id = ${submission.id}")
                val successor = expectNotNull(findSuccessorAsync(submission))
                log.trace("Newer submission found: id = ${successor.id}")
                message.submissionId = successor.id
                sendJsonableAsync(Address.Api.Submission.Comment.Create, message)
            }
            else -> throw IllegalArgumentException("Applying comment to an incorrect or incomplete submission")
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comment.Read)
    suspend fun handleCommentRead(message: SubmissionCommentRecord): SubmissionCommentRecord =
            dbFetchAsync(message)

    @JsonableEventBusConsumerFor(Address.Api.Submission.Comments)
    suspend fun handleComments(message: SubmissionRecord): List<SubmissionCommentRecord> =
            dbFindAsync(SubmissionCommentRecord().apply { submissionId = message.id })

}