package org.jetbrains.research.kotoed.data.api

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.UpdatableRecord

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
        fun Invalid(errors: List<Int>) = VerificationData(VerificationStatus.Invalid, errors)
        fun Invalid(vararg errors: Int) = Invalid(errors.asList())
    }
}

data class DbRecordWrapper(
        val record: JsonObject,
        val verificationData: VerificationData
) : Jsonable

inline fun <reified R : UpdatableRecord<R>> DbRecordWrapper(
        record: R,
        verificationData: VerificationData = VerificationData.Unknown
) = DbRecordWrapper(record.toJson(), verificationData)
