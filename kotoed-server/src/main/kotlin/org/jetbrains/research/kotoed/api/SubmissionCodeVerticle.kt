package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.Code.FileRecord
import org.jetbrains.research.kotoed.data.api.Code.FileType.directory
import org.jetbrains.research.kotoed.data.api.Code.FileType.file
import org.jetbrains.research.kotoed.data.api.Code.ListResponse
import org.jetbrains.research.kotoed.data.api.Code.Submission.RemoteRequest
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.data.api.Code.Course.ListRequest as CrsListRequest
import org.jetbrains.research.kotoed.data.api.Code.Course.ReadRequest as CrsReadRequest
import org.jetbrains.research.kotoed.data.api.Code.Course.ReadResponse as CrsReadResponse
import org.jetbrains.research.kotoed.data.api.Code.Submission.ListRequest as SubListRequest
import org.jetbrains.research.kotoed.data.api.Code.Submission.ReadRequest as SubReadRequest
import org.jetbrains.research.kotoed.data.api.Code.Submission.ReadResponse as SubReadResponse

private typealias InnerRemoteRequest = org.jetbrains.research.kotoed.data.vcs.RemoteRequest
private typealias InnerListRequest = org.jetbrains.research.kotoed.data.vcs.ListRequest
private typealias InnerListResponse = org.jetbrains.research.kotoed.data.vcs.ListResponse
private typealias InnerReadRequest = org.jetbrains.research.kotoed.data.vcs.ReadRequest
private typealias InnerReadResponse = org.jetbrains.research.kotoed.data.vcs.ReadResponse

@AutoDeployable
class SubmissionCodeVerticle : AbstractKotoedVerticle() {
    private data class CommitInfo(val repo: RepositoryInfo, val revision: String, val cloneStatus: CloneStatus) : Jsonable

    private suspend fun getCommitInfo(submission: SubmissionRecord): CommitInfo {
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

    private suspend fun getCommitInfo(course: CourseRecord): CommitInfo {
        if (course.id !is Int) throw IllegalArgumentException("Course $course not found")

        val repo: RepositoryInfo = sendJsonableAsync(
                Address.Code.Download,
                InnerRemoteRequest(
                        vcs = null, // FIXME: akhin Add course base repo type
                        url = course.baseRepoUrl
                )
        )
        return CommitInfo(repo, course.baseRevision, repo.status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Download)
    suspend fun handleSubmissionCodeDownload(message: RemoteRequest): RepositoryInfo {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        return getCommitInfo(submission).repo
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Code.Read)
    suspend fun handleCourseCodeRead(message: CrsReadRequest): CrsReadResponse {
        val course: CourseRecord = dbFetchAsync(CourseRecord().apply { id = message.courseId })
        val repoInfo = getCommitInfo(course)
        when (repoInfo.cloneStatus) {
            CloneStatus.pending -> return CrsReadResponse("", repoInfo.cloneStatus)
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
        return CrsReadResponse(contents = inner.contents, status = CloneStatus.done)
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Read)
    suspend fun handleSubmissionCodeRead(message: SubReadRequest): SubReadResponse {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        val repoInfo = getCommitInfo(submission)
        when (repoInfo.cloneStatus) {
            CloneStatus.pending -> return SubReadResponse("", repoInfo.cloneStatus)
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

        val from = (message.fromLine ?: 1) - 1
        val to = (message.toLine ?: inner.contents.lineSequence().count()) - 1
        val contents = inner.contents
                .lineSequence()
                .drop(from)
                .take(to - from + 1)
                .joinToString(separator = "\n")

        return SubReadResponse(contents = contents, status = CloneStatus.done)
    }

    // Feel da powa of Kotlin!
    private data class MutableCodeTree(
            private val data: MutableMap<String, MutableCodeTree> = mutableMapOf(),
            var changed: Boolean = false
    ) : MutableMap<String, MutableCodeTree> by data { // it's over 9000!

        private val fileComparator = compareBy<FileRecord> { it.type }.thenBy { it.name }

        private fun FileRecord.squash() =
                if (type == directory && children?.size == 1 && children[0].type == directory)
                    FileRecord(
                            type = directory,
                            name = "$name/${children[0].name}",
                            children = children[0].children,
                            changed = changed
                    )
                else
                    this

        private fun Map.Entry<String, MutableCodeTree>.toFileRecord(): FileRecord =
                if (value.isEmpty()) FileRecord(type = file, name = key, changed = value.changed)
                else FileRecord(
                        type = directory,
                        name = key,
                        children = value.map { it.toFileRecord() }.sortedWith(fileComparator),
                        changed = value.changed
                ).squash()


        fun toFileRecord() = FileRecord(
                type = directory,
                name = "",
                children = map { it.toFileRecord() }.sortedWith(fileComparator),
                changed = changed
        )
    }

    private fun buildCodeTree(files: List<String>, changedFiles: List<String>): FileRecord {
        val mutableCodeTree = MutableCodeTree()

        // this is not overly efficient, but who cares
        for (file in files) {
            val path = file.split('/', '\\')
            var current = mutableCodeTree
            for (crumb in path) {
                current = current.computeIfAbsent(crumb) { MutableCodeTree() }
            }
        }

        for (file in changedFiles) {
            val path = file.split('/', '\\')
            var current = mutableCodeTree
            for (crumb in path) {
                current.changed = true
                current = current[crumb] ?: break // do not mark removed files or "/dev/null"
                current.changed = true
            }
        }

        return mutableCodeTree.toFileRecord()
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Code.List)
    suspend fun handleCourseCodeList(message: CrsListRequest): ListResponse {
        val course: CourseRecord = dbFetchAsync(CourseRecord().apply { id = message.courseId })
        val repoInfo = getCommitInfo(course)
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
                root = buildCodeTree(
                        innerResp.files,
                        emptyList()
                ),
                status = repoInfo.cloneStatus
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.List)
    suspend fun handleSubmissionCodeList(message: SubListRequest): ListResponse {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        val repoInfo = getCommitInfo(submission)
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

        val closedSubs = dbFindAsync(SubmissionRecord().apply {
            projectId = submission.projectId
            state = SubmissionState.closed
        })

        val foundationSub = closedSubs.filter {
            it.datetime < submission.datetime
        }.sortedByDescending { it.datetime }.firstOrNull()

        var baseRev = foundationSub?.revision

        if (baseRev == null) {
            val course: CourseRecord =
                    dbQueryAsync(
                            ComplexDatabaseQuery(ProjectRecord().apply { id = submission.projectId }).join("course")
                    ).first().getJsonObject("course").toRecord()

            baseRev = if (course.baseRevision != "") course.baseRevision else null

            if (baseRev != null) try {
                run<Unit> {
                    sendJsonableAsync(
                            Address.Code.List,
                            InnerListRequest(
                                    uid = repoInfo.repo.uid,
                                    revision = baseRev
                            )
                    )
                }
            } catch (ex: Exception) {
                baseRev = null
            }
        }

        val diff: DiffResponse = when (baseRev) {
            null -> DiffResponse(listOf())
            else -> sendJsonableAsync(
                    Address.Code.Diff,
                    DiffRequest(
                            uid = repoInfo.repo.uid,
                            from = baseRev,
                            to = submission.revision
                    )
            )
        }

        return ListResponse(
                root = buildCodeTree(
                        innerResp.files,
                        diff.contents.flatMap { listOf(it.fromFile, it.toFile) }
                ),
                status = repoInfo.cloneStatus
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Date)
    suspend fun handleSubmissionCodeDate(message: SubReadRequest): BlameResponse {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        val repoInfo = getCommitInfo(submission)

        return sendJsonableAsync(
                Address.Code.Date,
                BlameRequest(
                        uid = repoInfo.repo.uid,
                        path = message.path,
                        fromLine = message.fromLine,
                        toLine = message.toLine
                )
        )

    }
}
