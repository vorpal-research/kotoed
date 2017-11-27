package org.jetbrains.research.kotoed.data.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.database.enums.SubmissionCommentState
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.TagRecord
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.tryToJson
import org.jooq.Record
import java.util.*

enum class VerificationStatus {
    Unknown,
    NotReady,
    Processed,
    Invalid
}

fun VerificationData?.bang() = this ?: VerificationData.Unknown

data class VerificationData(
        val status: VerificationStatus,
        val errors: List<Int>) : Jsonable {

    constructor(errors: List<Int>) : this(if (errors.isEmpty()) VerificationStatus.Processed else VerificationStatus.Invalid, errors)

    companion object {
        val Unknown = VerificationData(VerificationStatus.Unknown, listOf())
        val Processed = VerificationData(VerificationStatus.Processed, listOf())
        val NotReady = VerificationData(VerificationStatus.NotReady, listOf())
        fun Invalid(errors: List<Int>) = VerificationData(VerificationStatus.Invalid, errors)
        fun Invalid(vararg errors: Int) = Invalid(errors.asList())
    }

    infix fun and(other: VerificationData) =
            when {
                status == VerificationStatus.Invalid && other.status == VerificationStatus.Invalid -> {
                    Invalid(errors + other.errors)
                }
                status == VerificationStatus.Invalid -> this
                other.status == VerificationStatus.Invalid -> other
                else -> when (Pair(status, other.status)) {
                    Pair(VerificationStatus.NotReady, other.status),
                    Pair(status, VerificationStatus.NotReady) -> NotReady
                    Pair(VerificationStatus.Processed, VerificationStatus.Processed) -> Processed
                    else -> Unknown
                }
            }

}

data class DbRecordWrapper(
        val record: JsonObject,
        val verificationData: VerificationData
) : Jsonable

inline fun <reified R : Record> DbRecordWrapper(
        record: R,
        verificationData: VerificationData = VerificationData.Unknown
) = DbRecordWrapper(record.toJson(), verificationData)

data class DbRecordListWrapper(
        val records: JsonArray,
        val verificationData: VerificationData
) : Jsonable

inline fun <reified R : Record> DbRecordListWrapper(
        records: List<R>,
        verificationData: VerificationData = VerificationData.Unknown
) = DbRecordListWrapper(records.tryToJson() as JsonArray, verificationData)

object Code {
    object Submission {
        data class RemoteRequest(val submissionId: Int) : Jsonable
        data class ReadRequest(
                val submissionId: Int,
                val path: String,
                val fromLine: Int? = null,
                val toLine: Int? = null) : Jsonable
        data class ReadResponse(val contents: String, val status: CloneStatus) : Jsonable
        data class ListRequest(val submissionId: Int) : Jsonable
    }

    object Course {
        data class ReadRequest(val courseId: Int, val path: String) : Jsonable
        data class ReadResponse(val contents: String, val status: CloneStatus) : Jsonable
        data class ListRequest(val courseId: Int) : Jsonable
    }

    enum class FileType { directory, file } // directory < file, used in comparisons
    data class FileRecord(
            val type: FileType,
            val name: String,
            val children: List<FileRecord>? = null,
            val changed: Boolean = false) : Jsonable {
        fun toFileSeq(): Sequence<String> =
                when (type) {
                    FileType.directory ->
                        children
                                .orEmpty()
                                .asSequence()
                                .flatMap { it.toFileSeq() }
                                .map { "$name/$it" }
                    FileType.file -> sequenceOf(name)
                }.map { it.removePrefix("/") }
    }

    data class ListResponse(val root: FileRecord?, val status: CloneStatus) : Jsonable
}

object SubmissionComments {
    const val UnknownFile = "/dev/null"
    const val UnknownLine = 0

    data class LineComments(val line: Int, val comments: List<JsonObject>) : Jsonable
    data class FileComments(val filename: String, val byLine: List<LineComments>) : Jsonable

    data class CommentsResponse(val byFile: List<FileComments>, val lost: List<JsonObject>) : Jsonable

    data class CommentAggregate(
            private val map: MutableMap<SubmissionCommentState, Int> =
            EnumMap<SubmissionCommentState, Int>(SubmissionCommentState::class.java)) :
            MutableMap<SubmissionCommentState, Int> by map, Jsonable {

        init {
            for (state in SubmissionCommentState.values()) {
                map.computeIfAbsent(state) { 0 }
            } // Forcing zeroes to appear in JSON
        }

        fun register(comment: SubmissionCommentRecord) {
            this[comment.state] = this.getOrDefault(comment.state, 0) + 1
        }

        override fun toJson() = JsonObject(map.mapKeys { (k, _) -> k.toString() })
    }

    data class CommentAggregates(val byFile: Map<String, CommentAggregate>, val lost: CommentAggregate) : Jsonable {
        override fun toJson() = JsonObject(
                "by_file" to JsonArray(byFile.entries.map { e ->
                    JsonObject("file" to e.key, "aggregate" to e.value)
                }),
                "lost" to lost.toJson()
        )
    }

    data class LastSeenResponse(val location: SubmissionCommentRecord? = null) : Jsonable
}

object Submission {
    data class SubmissionHistoryQuery(val submissionId: Int, val limit: Int?) : Jsonable

    data class TagUpdateQuery(
            val submissionId: Int,
            val tags: List<TagRecord>) : Jsonable
}

interface PageableQuery {
    val currentPage: Int?
    val pageSize: Int?
}

data class SearchQuery(
        val text: String,
        val find: JsonObject?,
        override val currentPage: Int?,
        override val pageSize: Int?,
        val denizenId: Int?,
        val withVerificationData: Boolean?) : Jsonable, PageableQuery

data class SearchQueryWithTags(
        val text: String,
        val find: JsonObject?,
        override val currentPage: Int?,
        override val pageSize: Int?,
        val denizenId: Int?,
        val withVerificationData: Boolean?,
        val withTags: Boolean?) : Jsonable, PageableQuery


data class RestorePasswordSecret(
        val denizenId: String, val secret: String, val password: String
) : Jsonable

data class ProfileInfo(
        val id: Int,
        val denizenId: String,
        val email: String?,
        val oauth: Map<String, String?>,
        val firstName: String?,
        val lastName: String?,
        val group: String?
) : Jsonable

data class PasswordChangeRequest(
        val initiatorDenizenId: String,
        val initiatorPassword: String,
        val targetId: Int,
        val newPassword: String
) : Jsonable

data class ProfileInfoUpdate(
        val id: Int,
        val denizenId: String,
        val email: String?,
        val oauth: Map<String, String?>,
        val firstName: String?,
        val lastName: String?,
        val group: String?
) : Jsonable

enum class SubmissionCodeAnnotationSeverity { error, warning }

data class SubmissionCodeAnnotationPosition(
        val line: Int, val col: Int
) : Jsonable

data class SubmissionCodeAnnotation(
        val severity: SubmissionCodeAnnotationSeverity,
        val message: String,
        val position: SubmissionCodeAnnotationPosition
) : Jsonable

data class SubmissionCodeAnnotationResponse(
        val map: Map<String, Set<SubmissionCodeAnnotation>>
) : Jsonable
