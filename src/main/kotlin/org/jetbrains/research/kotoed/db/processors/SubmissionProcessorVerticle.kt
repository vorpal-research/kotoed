package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class SubmissionProcessorVerticle : ProcessorVerticle<SubmissionRecord>(Tables.SUBMISSION) {

    suspend override fun checkPrereqs(data: JsonObject): List<VerificationData> {

        val eb = vertx.eventBus()

        return table
                .references
                .asSequence()
                // XXX: we expect here that we have no composite foreign keys
                .filter { it.fieldsArray.size == 1 }
                .filter { it.key.fieldsArray.size == 1 }
                .map { fkey ->
                    val from = fkey.fieldsArray.first()
                    Pair(fkey.key, data[from])
                }
                .filter { it.second != null }
                .mapTo(mutableListOf<VerificationData>()) { (rkey, id) ->
                    val to = rkey.fieldsArray.first()
                    val toTable = rkey.table

                    eb.trySendAsync(Address.DB.verify(toTable.name), JsonObject(to.name to id))
                            ?.body()
                            ?.toJsonable()
                            ?: VerificationData.Processed
                }
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {
        return super.doProcess(data)
    }

    suspend override fun verify(data: JsonObject?): VerificationData {
        data ?: throw IllegalArgumentException("Cannot verify submission $data")
        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)

        val vcsReq: RepositoryInfo =
                sendJsonableAsync(
                        Address.Code.Download,
                        RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
                )

        val vcsStatus =
                when (vcsReq.status) {
                    CloneStatus.pending -> VerificationData.NotReady
                    CloneStatus.done -> VerificationData.Processed
                    CloneStatus.failed ->
                        dbCreateAsync(
                                SubmissionStatusRecord().apply {
                                    this.submissionId = sub.id
                                    this.data = JsonObject(
                                            "error" to "Fetching remote repository failed",
                                            "reason" to vcsReq.toJson()
                                    )
                                }
                        ).id.let { VerificationData.Invalid(it) }
                }

        if (vcsStatus != VerificationData.Processed) return vcsStatus

        try {
            forceType<ListResponse>(sendJsonableAsync(Address.Code.List, ListRequest(vcsReq.uid, sub.revision)))
        } catch (ex: ReplyException) {
            return dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "error" to "Fetching revision ${sub.revision} for repository ${project.repoUrl} failed",
                                "reason" to ex.message
                        )
                    }
            ).id.let { VerificationData.Invalid(it) }
        }

        return VerificationData.Processed
    }

}


