package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.core.eventbus.EventBus
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.fromJson
import org.jetbrains.research.kotoed.util.sendAsync

suspend fun EventBus.commentById(id: Int): SubmissionCommentRecord {
    val commentWrapper = fromJson<DbRecordWrapper>(this.sendAsync(
            org.jetbrains.research.kotoed.eventbus.Address.Api.Submission.Comment.Read,
            org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord().apply { this.id = id }.toJson()).body())

    return commentWrapper.record.toRecord<SubmissionCommentRecord>()
}

suspend fun EventBus.submissionById(id: Int): SubmissionRecord {
    val commentWrapper = fromJson<DbRecordWrapper>(this.sendAsync(
            org.jetbrains.research.kotoed.eventbus.Address.Api.Submission.Read,
            org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord().apply { this.id = id }.toJson()).body())

    return commentWrapper.record.toRecord<SubmissionRecord>()
}