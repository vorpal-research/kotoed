package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.SubmissionCode.FileRecord
import org.jetbrains.research.kotoed.data.api.SubmissionCode.FileType.directory
import org.jetbrains.research.kotoed.data.api.SubmissionCode.FileType.file
import org.jetbrains.research.kotoed.data.api.SubmissionCode.ListRequest
import org.jetbrains.research.kotoed.data.api.SubmissionCode.ListResponse
import org.jetbrains.research.kotoed.data.api.SubmissionCode.ReadRequest
import org.jetbrains.research.kotoed.data.api.SubmissionCode.ReadResponse
import org.jetbrains.research.kotoed.data.api.SubmissionCode.RemoteRequest
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.data.vcs.RepositoryInfo
import org.jetbrains.research.kotoed.data.vcs.VCS
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

private typealias InnerRemoteRequest = org.jetbrains.research.kotoed.data.vcs.RemoteRequest
private typealias InnerListRequest = org.jetbrains.research.kotoed.data.vcs.ListRequest
private typealias InnerListResponse = org.jetbrains.research.kotoed.data.vcs.ListResponse
private typealias InnerReadRequest = org.jetbrains.research.kotoed.data.vcs.ReadRequest
private typealias InnerReadResponse = org.jetbrains.research.kotoed.data.vcs.ReadResponse

@AutoDeployable
class SubmissionCodeVerticle : AbstractKotoedVerticle() {
    private data class CommitInfo(val repo: RepositoryInfo, val revision: String, val cloneStatus: CloneStatus) : Jsonable

    private suspend fun getCommitInfo(submissionId: Int): CommitInfo {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = submissionId })
        if (submission.id !is Int) throw IllegalArgumentException("Submission $submission not found")
        val project = dbFetchAsync(ProjectRecord().apply { id = submission.projectId })
        if (project.id !is Int) throw IllegalStateException("Invalid project: $project")

        val repo: RepositoryInfo = sendJsonableAsync(
                Address.Code.Download,
                InnerRemoteRequest(
                        vcs = tryOrNull { VCS.valueOf(project.repoType) },
                        url = project.repoUrl
                )
        )
        return CommitInfo(repo, submission.revision, repo.status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Download)
    suspend fun handleSubmissionCodeDownload(message: RemoteRequest): RepositoryInfo {
        return getCommitInfo(message.submissionId).repo
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Read)
    suspend fun handleSubmissionCodeRead(message: ReadRequest): ReadResponse {
        val repoInfo = getCommitInfo(message.submissionId)
        when (repoInfo.cloneStatus) {
            CloneStatus.pending -> return ReadResponse("", repoInfo.cloneStatus)
            CloneStatus.failed -> throw NotFound("Repository not found")
            else -> {
            }
        }

        val inner: InnerReadResponse = sendJsonableAsync(
                Address.Code.Read,
                InnerReadRequest(
                        path = message.path,
                        uid = repoInfo.repo.uid,
                        revision = repoInfo.revision
                )
        )
        return ReadResponse(contents = inner.contents, status = CloneStatus.done)
    }

    // Feel da powa of Kotlin!
    private data class MutableCodeTree(
            private val data: MutableMap<String, MutableCodeTree> = mutableMapOf()
    ) : MutableMap<String, MutableCodeTree> by data { // it's over 9000!

        private val fileComparator = compareBy<FileRecord> { it.type }.thenBy { it.name }

        private fun Map.Entry<String, MutableCodeTree>.toFileRecord(): FileRecord =
                if (value.isEmpty()) FileRecord(type = file, name = key)
                else FileRecord(
                        type = directory,
                        name = key,
                        children = value.map { it.toFileRecord() }.sortedWith(fileComparator)
                )

        fun toFileRecord() = FileRecord(
                type = directory,
                name = "",
                children = map { it.toFileRecord() }.sortedWith(fileComparator)
        )
    }

    private fun buildCodeTree(files: List<String>): FileRecord {
        val mutableCodeTree = MutableCodeTree()

        // this is not overly efficient, but who cares
        for (file in files) {
            val path = file.split('/', '\\')
            var current = mutableCodeTree
            for (crumb in path) {
                current = current.computeIfAbsent(crumb) { MutableCodeTree() }
            }
        }

        return mutableCodeTree.toFileRecord()
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.List)
    suspend fun handleSubmissionCodeList(message: ListRequest): ListResponse {
        val repoInfo = getCommitInfo(message.submissionId)
        when (repoInfo.cloneStatus) {
            CloneStatus.pending -> return ListResponse(root = null, status = repoInfo.cloneStatus)
            CloneStatus.failed -> throw NotFound("Repository not found")
            else -> {
            }
        }

        val innerResp: InnerListResponse = sendJsonableAsync(
                Address.Code.List,
                InnerListRequest(
                        uid = repoInfo.repo.uid,
                        revision = repoInfo.revision
                )
        )

        return ListResponse(
                root = buildCodeTree(innerResp.files),
                status = repoInfo.cloneStatus
        )
    }
}