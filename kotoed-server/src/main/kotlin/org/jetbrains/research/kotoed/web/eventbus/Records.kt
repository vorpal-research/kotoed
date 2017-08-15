package org.jetbrains.research.kotoed.web.eventbus

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.SubmissionComment
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.util.database.toRecord

import org.jetbrains.research.kotoed.util.sendJsonableAsync

suspend fun EventBus.commentByIdOrNull(id: Int): SubmissionCommentRecord? {
    val comment: SubmissionCommentRecord
    try {
        comment = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(Tables.SUBMISSION_COMMENT.name),
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
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(Tables.SUBMISSION.name),
                org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    return submission
}

suspend fun EventBus.projectByIdOrNull(id: Int): ProjectRecord? {
    val project: ProjectRecord
    try {
        project = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(Tables.PROJECT.name),
                org.jetbrains.research.kotoed.database.tables.records.ProjectRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    return project
}

suspend fun EventBus.denizenByIdOrNull(id: Int): DenizenRecord? {
    val denizen: DenizenRecord
    try {
        denizen = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(Tables.DENIZEN.name),
                org.jetbrains.research.kotoed.database.tables.records.ProjectRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    return denizen
}

suspend fun EventBus.courseByIdOrNull(id: Int): CourseRecord? {
    val course: CourseRecord
    try {
        course = this.sendJsonableAsync(
                org.jetbrains.research.kotoed.eventbus.Address.DB.read(Tables.COURSE.name),
                org.jetbrains.research.kotoed.database.tables.records.CourseRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    return course
}

