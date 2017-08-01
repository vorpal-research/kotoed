package org.jetbrains.research.kotoed.data.api

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.database.enums.Submissioncommentstate
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.Record
import org.jooq.TableRecord
import org.jooq.UpdatableRecord
import java.time.Instant

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
            when{
                status == VerificationStatus.Invalid && other.status == VerificationStatus.Invalid -> {
                    Invalid(errors + other.errors)
                }
                status == VerificationStatus.Invalid -> this
                other.status == VerificationStatus.Invalid -> other
                else -> when(Pair(status, other.status)) {
                    Pair(VerificationStatus.NotReady, other.status),
                        Pair(status, VerificationStatus.NotReady) -> NotReady
                    Pair(VerificationStatus.Unknown, other.status),
                        Pair(status, VerificationStatus.Unknown) -> NotReady
                    Pair(VerificationStatus.Processed, VerificationStatus.Processed) ->
                        Processed
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

object SubmissionCode {
    data class RemoteRequest(val submissionId: Int): Jsonable
    data class ReadRequest(val submissionId: Int, val path: String): Jsonable
    data class ReadResponse(val contents: String, val status: CloneStatus): Jsonable
    data class ListRequest(val submissionId: Int): Jsonable

    enum class FileType { directory, file } // directory < file, used in comparisons
    data class FileRecord(val type: FileType, val name: String, val children: List<FileRecord>? = null): Jsonable
    data class ListResponse(val root: FileRecord?, val status: CloneStatus): Jsonable
}

object SubmissionComments {
    data class LineComments(val line: Int, val comments: List<JsonObject>): Jsonable
    data class FileComments(val filename: String, val byLine: List<LineComments>): Jsonable
}