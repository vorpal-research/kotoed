package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.SearchQuery
import org.jetbrains.research.kotoed.data.api.SearchQueryWithTags
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.query
import org.jetbrains.research.kotoed.data.db.setPageForQuery
import org.jetbrains.research.kotoed.data.db.textSearch
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectTextSearchRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.TableRecord

@AutoDeployable
class ProjectVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Project.Create)
    suspend fun handleCreate(project: ProjectRecord): DbRecordWrapper {
        project.name = project.name.truncateAt(1024)
        project.id = null
        expect(project.courseId is Int)
        expect(project.denizenId is Int)

        val res: ProjectRecord = dbCreateAsync(project)
        dbProcessAsync(res)
        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.Read)
    suspend fun handleRead(project: ProjectRecord): DbRecordWrapper {
        val res: ProjectRecord = dbFetchAsync(project)
        val status: VerificationData = dbProcessAsync(res)
        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.Delete)
    suspend fun handleDelete(project: ProjectRecord): Unit {
        log.info("Deleting project ${project.id}!")
        run { dbUpdateAsync(ProjectRecord().apply { id = project.id; deleted = true }) }
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.Error)
    suspend fun handleError(verificationData: VerificationData): List<ProjectStatusRecord> =
            verificationData.errors
                    .map { fetchByIdAsync(Tables.PROJECT_STATUS, it) }

    @JsonableEventBusConsumerFor(Address.Api.Project.Verification.Data)
    suspend fun handleVerificationData(project: ProjectRecord): VerificationData =
            dbVerifyAsync(project)

    @JsonableEventBusConsumerFor(Address.Api.Project.SearchForCourse)
    suspend fun handleSearchForCourse(query: SearchQueryWithTags): JsonArray {
        val subQ = query(Tables.SUBMISSION) {
            find {
                state = SubmissionState.open
            }

            sortBy("-datetime")

            if(query.withTags == true) {
                rjoin(Tables.SUBMISSION_TAG) {
                    join(Tables.TAG)
                }
            }
        }

        val tableName =
                if(query.withTags == true)
                    Tables.PROJECT_TEXT_SEARCH.name
                else Tables.PROJECT_RESTRICTED_TEXT_SEARCH.name

        val q = ComplexDatabaseQuery(tableName)
                .find(ProjectRecord().apply {
                    deleted = false
                })
                .join(ComplexDatabaseQuery(Tables.DENIZEN).rjoin(Tables.PROFILE))
                .rjoin(subQ, "project_id", "openSubmissions")
                .find(ProjectTextSearchRecord().apply {
                    courseId = query.find?.getInteger(Tables.PROJECT.COURSE_ID.name)
                    denizenId = query.find?.getInteger(Tables.PROJECT.DENIZEN_ID.name)
                    if(query.withTags == true && "!empty" in query.text.split(Regex("""\s+"""))) {
                        empty = false
                    }
                })
                .textSearch(query.text)
                .setPageForQuery(query)

        val projects: List<JsonObject> =
                sendJsonableCollectAsync(Address.DB.query(tableName), q)

        val reqWithVerificationData = if (query.withVerificationData ?: false) {
            projects.map { json ->
                val record: ProjectRecord = json.toRecord()
                val vd = dbProcessAsync(record)

                json["verificationData"] = vd.toJson()
                json["openSubmissions"] = json.getJsonArray("openSubmissions")
                        ?.map {
                            val sub: SubmissionRecord = (it as JsonObject).toRecord()
                            val vdSub = dbProcessAsync(sub)
                            it["verificationData"] = vdSub.toJson()
                            it
                        }?: JsonArray()
                json
            }
        } else projects

        return JsonArray(reqWithVerificationData)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.SearchForCourseCount)
    suspend fun handleSearchForCourseCount(query: SearchQueryWithTags): JsonObject {
        val tableName =
                if(query.withTags == true)
                    Tables.PROJECT_TEXT_SEARCH.name
                else Tables.PROJECT_RESTRICTED_TEXT_SEARCH.name


        val q = ComplexDatabaseQuery(tableName)
                .find(ProjectRecord().apply{ deleted = false })
                .join(Tables.DENIZEN.name)
                .find(ProjectTextSearchRecord().apply {
                    courseId = query.find?.getInteger(Tables.PROJECT.COURSE_ID.name)
                    denizenId = query.find?.getInteger(Tables.PROJECT.DENIZEN_ID.name)
                    if(query.withTags == true && "!empty" in query.text.split(Regex("""\s+"""))) {
                        empty = false
                    }
                })
                .textSearch(query.text)

        return sendJsonableAsync(Address.DB.count(tableName), q)
    }
}
