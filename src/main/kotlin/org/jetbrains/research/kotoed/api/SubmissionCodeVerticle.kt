package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@AutoDeployable
class SubmissionCodeVerticle: AbstractKotoedVerticle() {
    private data class CommitInfo(val repo: RepositoryInfo, val revision: String): Jsonable

    private suspend fun getCommitInfo(submissionId: Int): CommitInfo {
        val submission: SubmissionRecord = sendJsonableAsync(
                Address.Api.Submission.Read,
                SubmissionRecord().apply { id = submissionId }
        )
        if(submission.id !is Int) throw IllegalArgumentException("Submission $submission not found")
        val project = dbFetchAsync(ProjectRecord().apply { id = submission.projectId })
        if(project.id !is Int) throw IllegalStateException("Invalid project: $project")

        val repo: RepositoryInfo = sendJsonableAsync(
                Address.Code.Download,
                RemoteRequest(
                        vcs = tryOrNull { VCS.valueOf(project.repoType)  },
                        url = project.repoUrl
                )
        )
        return CommitInfo(repo, submission.revision)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Download)
    suspend fun handleSubmissionCodeDownload(message: SubmissionCodeRemoteRequest): RepositoryInfo {
        return getCommitInfo(message.submissionId).repo
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Read)
    suspend fun handleSubmissionCodeRead(message: SubmissionCodeReadRequest): SubmissionCodeReadResponse {
        val repoInfo = getCommitInfo(message.submissionId)

        return sendJsonableAsync(
                Address.Code.Read,
                ReadRequest(
                        path = message.path,
                        uid = repoInfo.repo.uid,
                        revision = repoInfo.revision
                )
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.List)
    suspend fun handleSubmissionCodeList(message: SubmissionCodeListRequest): SubmissionCodeListResponse {
        val repoInfo = getCommitInfo(message.submissionId)

        return sendJsonableAsync(
                Address.Code.List,
                ListRequest(
                        uid = repoInfo.repo.uid,
                        revision = repoInfo.revision
                )
        )
    }
}