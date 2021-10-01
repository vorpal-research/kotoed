package org.jetbrains.research.kotoed.web.eventbus

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import org.jetbrains.kotlin.utils.join
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.DatabaseJoin
import org.jetbrains.research.kotoed.data.db.query
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.*
import org.jetbrains.research.kotoed.database.tables.SubmissionComment
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Table
import org.jooq.TableRecord

inline suspend fun <reified R: TableRecord<R>> WithVertx.fetchByIdOrNull(table: Table<R>, id: Int): R? {
    val result: R
    try {
        result = fetchByIdAsync(table, id)
    } catch (ex: ReplyException) {
        if (ex.failureCode() == HttpResponseStatus.NOT_FOUND.code())
            return null
        else throw ex
    }

    return result
}

suspend fun WithVertx.commentByIdOrNull(id: Int): SubmissionCommentRecord? =
    fetchByIdOrNull(SUBMISSION_COMMENT, id)

suspend fun WithVertx.submissionByIdOrNull(id: Int): SubmissionRecord? =
    fetchByIdOrNull(SUBMISSION, id)

suspend fun WithVertx.projectByIdOrNull(id: Int): ProjectRecord? =
    fetchByIdOrNull(PROJECT, id)

suspend fun WithVertx.denizenByIdOrNull(id: Int): DenizenRecord? =
    fetchByIdOrNull(DENIZEN, id)

suspend fun WithVertx.courseByIdOrNull(id: Int): CourseRecord? =
    fetchByIdOrNull(COURSE, id)

suspend fun WithVertx.buildTemplateByIdOrNull(id: Int): BuildTemplateRecord? =
    fetchByIdOrNull(BUILD_TEMPLATE, id)

suspend fun WithVertx.notificationByIdOrNull(id: Int): NotificationRecord? =
    fetchByIdOrNull(NOTIFICATION, id)

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
