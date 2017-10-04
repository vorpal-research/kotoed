package org.jetbrains.research.kotoed.web.eventbus

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import org.jetbrains.kotlin.utils.join
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.DatabaseJoin
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.COURSE
import org.jetbrains.research.kotoed.database.Tables.DENIZEN
import org.jetbrains.research.kotoed.database.Tables.PROFILE
import org.jetbrains.research.kotoed.database.Tables.PROJECT
import org.jetbrains.research.kotoed.database.Tables.SUBMISSION
import org.jetbrains.research.kotoed.database.tables.SubmissionComment
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord

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

suspend fun EventBus.notificationByIdOrNull(id: Int): NotificationRecord? {
    val notification: NotificationRecord
    try {
        notification = this.sendJsonableAsync(
                Address.DB.read(Tables.NOTIFICATION.name),
                NotificationRecord().apply { this.id = id })
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }
    return notification
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Complex stuff
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class ProjectWithRelated(
        val course: CourseRecord,
        val author: DenizenRecord,
        val authorProfile: ProfileRecord?,
        val project: ProjectRecord) {
    companion object {

        fun makeQuery(find: ProjectRecord? = null): ComplexDatabaseQuery {
            val q = ComplexDatabaseQuery(PROJECT.name)
                    .join(ComplexDatabaseQuery(Tables.DENIZEN).rjoin(Tables.PROFILE), resultField = "author")
                    .join(COURSE.name)

            return find?.let {
                q.find(it)
            } ?: q
        }

        suspend fun fetchByIdOrNull(eventBus: EventBus, id: Int): ProjectWithRelated? {
            val query = makeQuery(ProjectRecord().apply { this.id = id })

            val res: List<JsonObject> = eventBus.sendJsonableCollectAsync(Address.DB.query(PROJECT.name), query)

            return res.firstOrNull()?.run { Companion.fromJson(this) }
        }

        fun fromJson(obj: JsonObject): ProjectWithRelated =
                ProjectWithRelated(
                        obj.getJsonObject("course").toRecord(),
                        obj.getJsonObject("author").toRecord(),
                        obj.getJsonObject("author")
                                ?.getJsonArray("profiles")
                                ?.firstOrNull()
                                .uncheckedCastOrNull<JsonObject>()
                                ?.toRecord(),
                        obj.toRecord()
                )
    }
}

data class SubmissionWithRelated(val course: CourseRecord,
                                 val author: DenizenRecord,
                                 val authorProfile: ProfileRecord?,
                                 val project: ProjectRecord,
                                 val submission: SubmissionRecord) {
    companion object {
        suspend fun fetchByIdOrNull(eventBus: EventBus, id: Int): SubmissionWithRelated? {
            val query = ComplexDatabaseQuery(
                    SUBMISSION.name,
                    joins = listOf(DatabaseJoin(query = ProjectWithRelated.makeQuery())))
                    .find(ProjectRecord().apply { this.id = id })

            val res: List<JsonObject> = eventBus.sendJsonableCollectAsync(Address.DB.query(PROJECT.name), query)

            return res.firstOrNull()?.run { Companion.fromJson(this) }
        }

        private fun fromJson(obj: JsonObject): SubmissionWithRelated {
            val pwr = ProjectWithRelated.fromJson(obj.getJsonObject("project"))
            return SubmissionWithRelated(
                    pwr.course,
                    pwr.author,
                    pwr.authorProfile,
                    pwr.project,
                    obj.toRecord())
        }
    }
}

data class DenizenWithProfile(val denizen: DenizenRecord,
                              val profile: ProfileRecord?) {
    companion object {
        suspend fun fetchByIdOrNull(eventBus: EventBus, id: Int): DenizenWithProfile? {
            val query = ComplexDatabaseQuery(
                    DENIZEN.name)
                    .find(ProjectRecord().apply { this.id = id })
                    .rjoin(PROFILE.name)

            val res: List<JsonObject> = eventBus.sendJsonableCollectAsync(Address.DB.query(PROJECT.name), query)

            return res.firstOrNull()?.run { Companion.fromJson(this) }
        }

        private fun fromJson(obj: JsonObject): DenizenWithProfile {
            return DenizenWithProfile(
                    denizen = obj.toRecord(),
                    profile = obj.getJsonArray("profiles")
                            ?.firstOrNull()
                            ?.uncheckedCastOrNull<JsonObject>()
                            ?.toRecord())
        }
    }
}
