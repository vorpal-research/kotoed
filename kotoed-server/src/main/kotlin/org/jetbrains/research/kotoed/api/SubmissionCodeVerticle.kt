package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.Code
import org.jetbrains.research.kotoed.data.api.Code.FileRecord
import org.jetbrains.research.kotoed.data.api.Code.FileType.directory
import org.jetbrains.research.kotoed.data.api.Code.FileType.file
import org.jetbrains.research.kotoed.data.api.Code.ListResponse
import org.jetbrains.research.kotoed.data.api.Code.Submission.RemoteRequest
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.TagRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.data.api.Code.Course.ListRequest as CrsListRequest
import org.jetbrains.research.kotoed.data.api.Code.Course.ReadRequest as CrsReadRequest
import org.jetbrains.research.kotoed.data.api.Code.Course.ReadResponse as CrsReadResponse
import org.jetbrains.research.kotoed.data.api.Code.Submission.ListRequest as SubListRequest
import org.jetbrains.research.kotoed.data.api.Code.Submission.ReadRequest as SubReadRequest
import org.jetbrains.research.kotoed.data.api.Code.Submission.ReadResponse as SubReadResponse
import org.jetbrains.research.kotoed.data.api.Code.Submission.DiffRequest as SubDiffRequest
import org.jetbrains.research.kotoed.data.api.Code.Submission.DiffResponse as SubDiffResponse

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
        val course = dbFetchAsync(CourseRecord().apply { id = project.courseId })
        if (course.id !is Int) throw IllegalStateException("Invalid course: $course")

        val repo: RepositoryInfo = sendJsonableAsync(
                Address.Code.Download,
                InnerRemoteRequest(
                        vcs = tryOrNull { VCS.valueOf(project.repoType) },
                        url = project.repoUrl
                )
        )
        if (course.baseRepoUrl != null && repo.status == CloneStatus.done) {
            val courseRepo = getCommitInfo(course).repo
            if (courseRepo.status == CloneStatus.done) {
                run<Unit> { sendJsonableAsync(Address.Code.Fetch,
                    FetchRequest(uid = repo.uid, externalUid = courseRepo.uid)) }
            }
        }

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

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.Diff)
    suspend fun handleSubmissionCodeDiff(message: SubDiffRequest): SubDiffResponse {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        val repoInfo = getCommitInfo(submission)
        val toRevInfo = Code.Submission.RevisionInfo(submission)

        when (repoInfo.cloneStatus) {
            CloneStatus.pending -> return SubDiffResponse(
                    diff = emptyList(),
                    status = repoInfo.cloneStatus,
                    from = toRevInfo,
                    to = toRevInfo
            )
            CloneStatus.failed -> throw NotFound("Repository not found")
            else -> {
            }
        }
        val baseRev = message.base.getBaseRev(submission)
        val diff = when (baseRev) {
            null -> DiffResponse(listOf())
            else -> sendJsonableAsync(
                    Address.Code.Diff,
                    DiffRequest(
                            uid = repoInfo.repo.uid,
                            from = baseRev.revision,
                            to = submission.revision
                    )
            )
        }

        return SubDiffResponse(
                diff = diff.contents,
                status = repoInfo.cloneStatus,
                from = baseRev ?: toRevInfo, // Makes sense for an empty diff
                to = toRevInfo
        )
    }

    // Feel da powa of Kotlin!
    private data class MutableCodeTree(
            private val data: MutableMap<String, MutableCodeTree> = mutableMapOf(),
    ) : MutableMap<String, MutableCodeTree> by data { // it's over 9000!

        private val fileComparator = compareBy<FileRecord> { it.type }.thenBy { it.name }

        private fun FileRecord.squash() =
                if (type == directory && children?.size == 1 && children[0].type == directory)
                    FileRecord(
                            type = directory,
                            name = "$name/${children[0].name}",
                            children = children[0].children
                    )
                else
                    this

        private fun Map.Entry<String, MutableCodeTree>.toFileRecord(): FileRecord =
                if (value.isEmpty()) FileRecord(type = file, name = key)
                else FileRecord(
                        type = directory,
                        name = key,
                        children = value.map { it.toFileRecord() }.sortedWith(fileComparator)
                ).squash()


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
                        innerResp.files
                ),
                status = repoInfo.cloneStatus
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Submission.Code.List)
    suspend fun handleSubmissionCodeList(message: SubListRequest): ListResponse {
        val submission: SubmissionRecord = dbFetchAsync(SubmissionRecord().apply { id = message.submissionId })
        val repoInfo = getCommitInfo(submission)
        val base = message.diffBase

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
                        innerResp.files
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

    private suspend fun SubDiffRequest.DiffBase.getBaseRev(submission: SubmissionRecord): Code.Submission.RevisionInfo? =
            when (type) {
                Code.Submission.DiffBaseType.SUBMISSION_ID -> dbFindAsync(SubmissionRecord().apply {
                    projectId = submission.projectId
                    id = submissionId
                }).firstOrNull()?.revision?.let {
                    Code.Submission.RevisionInfo(it)
                }
                Code.Submission.DiffBaseType.PREVIOUS_CHECKED -> submission.getPreviousChecked()
                Code.Submission.DiffBaseType.PREVIOUS_CLOSED -> submission.getPreviousClosed()
                Code.Submission.DiffBaseType.COURSE_BASE -> submission.getCourseBaseRev()
            }
    private suspend fun SubmissionRecord.getLatestClosedSub(): SubmissionRecord? =
            dbQueryAsync(
                    ComplexDatabaseQuery(Tables.SUBMISSION)
                            .filter("project_id == %s and state == %s and datetime < %s"
                                    .formatToQuery(projectId, SubmissionState.closed, datetime))
            )

                    .asSequence()
                    .map {
                        it.toRecord<SubmissionRecord>()
                    }
                    .filter {
                        it.datetime < datetime
                    }
                    .sortedByDescending {
                        it.datetime
                    }
                    .firstOrNull()
    private suspend fun SubmissionRecord.getCourseBaseRev(): Code.Submission.RevisionInfo? =
            dbQueryAsync(
                    ComplexDatabaseQuery(ProjectRecord().apply { id = projectId }).join("course")
            )
                    .first()
                    .getJsonObject("course")
                    .toRecord<CourseRecord>()
                    .let {
                        if (it.baseRevision != "") Code.Submission.RevisionInfo(it.baseRevision) else null
                    }

    private suspend fun SubmissionRecord.getPreviousChecked(): Code.Submission.RevisionInfo? {
        this.
        val latestClosed = getLatestClosedSub() // We consider closed as checked here
        val q = "project_id == %s " +
                (latestClosed?.datetime?.let { "and datetime > %s" } ?: "")
        val qArgs = sequence {
            yield(projectId)
            latestClosed?.datetime?.let {
                yield(it)
            }
        }.toList().toTypedArray()

        val newerThanClosed = dbQueryAsync(
                ComplexDatabaseQuery(Tables.SUBMISSION)
                        .rjoin(ComplexDatabaseQuery(Tables.SUBMISSION_TAG)
                                .join(Tables.TAG), "submission_id", "tags")
                        .filter(q.formatToQuery(*qArgs))
        )

        val byId = newerThanClosed
                .asSequence().map {
                    it.toRecord(SubmissionRecord::class)
                }.associateBy {
                    it.id
                }

        val tagsById = newerThanClosed
                .asSequence()
                .map {
                    it.getInteger("id") to it.getJsonArray("tags").asSequence().map {
                        it.uncheckedCast<JsonObject>().getJsonObject("tag").toRecord<TagRecord>().name
                    }.toSet()
                }
                .toMap()

        var current = this
        do {
            current = byId[current.parentSubmissionId] ?: return latestClosed?.let {
                Code.Submission.RevisionInfo(it)
            }
            val tags = tagsById[current.id] ?: return latestClosed?.let {
                Code.Submission.RevisionInfo(it)
            }
            if (CHECKED in tags) return Code.Submission.RevisionInfo(current)
        } while(true)

    }
    private suspend fun SubmissionRecord.getPreviousClosed(): Code.Submission.RevisionInfo? =
            getLatestClosedSub()?.let { Code.Submission.RevisionInfo(it) } ?: getCourseBaseRev()

    companion object {
        private const val CHECKED = "checked"
    }
}
