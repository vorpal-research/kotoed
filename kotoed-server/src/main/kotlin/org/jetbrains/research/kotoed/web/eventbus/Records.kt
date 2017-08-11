package org.jetbrains.research.kotoed.web.eventbus

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.database.tables.SubmissionComment
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.util.database.toRecord

import org.jetbrains.research.kotoed.util.sendJsonableAsync

suspend fun EventBus.commentByIdOrNull(id: Int): SubmissionCommentRecord? {
    val comment: SubmissionCommentRecord
    try {
        comment = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(SubmissionCommentRecord().table.name),
                org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }

    return comment
}

suspend fun EventBus.submissionByIdOrNull(id: Int): SubmissionRecord? {
    val submission: SubmissionRecord
    try {
        submission = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(SubmissionRecord().table.name),
                org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    println(submission)
    return submission
}