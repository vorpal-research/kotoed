package org.jetbrains.research.kotoed.api

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.SearchQuery
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Project.Create)
    suspend fun handleCreate(project: ProjectRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

        project.name = project.name.truncateAt(1024)
        project.id = null
        expect(project.courseId is Int)
        expect(project.denizenId is Int)

        val res: ProjectRecord = dbCreateAsync(project)

        eb.send(Address.DB.process(project.table.name), res.toJson())

        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.Read)
    suspend fun handleRead(project: ProjectRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

        val res: ProjectRecord = vxa<Message<JsonObject>> {
            eb.send(Address.DB.read(project.table.name), project.toJson(), it)
        }.body().toRecord()

        val status: VerificationData = vxa<Message<JsonObject>> {
            eb.send(Address.DB.process(project.table.name), res.toJson(), it)
        }.body().toJsonable()

        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.Error)
    suspend fun handleError(verificationData: VerificationData): List<ProjectStatusRecord> =
            verificationData.errors
                    .map { fetchByIdAsync(Tables.PROJECT_STATUS, it) }

    @JsonableEventBusConsumerFor(Address.Api.Project.Verification.Data)
    suspend fun handleVerificationData(project: ProjectRecord): VerificationData =
            dbVerifyAsync(project)

    @JsonableEventBusConsumerFor(Address.Api.Project.Search)
    suspend fun handleSearch(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE
        val currentPage = query.currentPage ?: 0
        val projQ = ComplexDatabaseQuery("project_text_search")
                .join("denizen", field = "denizen_id")
                .join("course", field = "course_id")

        val q = ComplexDatabaseQuery("submission")
                .find(SubmissionRecord().apply { state = SubmissionState.open })
                .join(projQ, field = "project_id")
                .filter("project.document matches %s".formatToQuery(query.text))
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val req: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query("submission"), q)

        return req.map { sub ->
            val project = sub.getJsonObject("project")
            project.toRecord<ProjectRecord>().toJson().apply {
                put("last_submission_id", sub["id"])
                put("denizen_id", project["denizen", "denizen_id"])
                put("course_name", project["course", "name"])
            }
        }.let(::JsonArray)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): JsonObject {
        val projQ = ComplexDatabaseQuery("project_text_search")
                .join("denizen", field = "denizen_id")
                .join("course", field = "course_id")

        val q = ComplexDatabaseQuery("submission")
                .find(SubmissionRecord().apply { state = SubmissionState.open })
                .join(projQ, field = "project_id")
                .filter("project.document matches %s".formatToQuery(query.text))

        return sendJsonableAsync(Address.DB.count("submission"), q)
    }


    // TODO maybe we should have only one universal search for project?
    @JsonableEventBusConsumerFor(Address.Api.Project.SearchForCourse)
    suspend fun handleSearchForCourse(query: SearchQuery): JsonArray {
        val pageSize = query.pageSize ?: Int.MAX_VALUE
        val currentPage = query.currentPage ?: 0
        val q = ComplexDatabaseQuery(Tables.PROJECT_TEXT_SEARCH.name)
                .join(Tables.DENIZEN.name)
                .find(ProjectRecord().apply {
                    courseId = query.find?.getInteger(Tables.PROJECT.COURSE_ID.name)
                    denizenId = query.find?.getInteger(Tables.PROJECT.DENIZEN_ID.name)
                })
                .limit(pageSize)
                .offset(currentPage * pageSize)

        val qWithSearch = if (query.text == "") q else q.filter("document matches %s".formatToQuery(query.text))

        val projects: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query(Tables.COURSE.name), qWithSearch)

        val subQ = ComplexDatabaseQuery(Tables.SUBMISSION.name)
                .find(SubmissionRecord().apply {
                    state = SubmissionState.open
                })
                .filter("${Tables.SUBMISSION.PROJECT_ID.name} in %s".formatToQuery(projects.map { it.getInteger("id") }))

        val submissionsJson: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query(Tables.SUBMISSION.name), subQ)
        // TODO verification data for submissions
        val submissionsByProject = submissionsJson
                .map { it.toRecord<SubmissionRecord>() }
                .groupBy { it.projectId }
                .mapValues { (_, v) -> v.sortedBy { it.datetime } }

        val reqWithVerificationData = if (query.withVerificationData ?: false) {
            projects.map { json ->
                val record: ProjectRecord = json.toRecord()
                val vd = dbProcessAsync(record)
                json["verificationData"] = vd.toJson()
                json["openSubmissions"] = submissionsByProject[record.id]
                        ?.map {
                            val vdSub = dbProcessAsync(it)
                            val subJson = it.toJson()
                            subJson["verificationData"] = vdSub.toJson()
                            subJson
                        }
                        ?.let(::JsonArray)
                        ?: JsonArray()
                json
            }
        } else projects

        return JsonArray(reqWithVerificationData)
    }

    @JsonableEventBusConsumerFor(Address.Api.Project.SearchForCourseCount)
    suspend fun handleSearchForCourseCount(query: SearchQuery): JsonObject {
        val q = ComplexDatabaseQuery(Tables.PROJECT_TEXT_SEARCH.name)
                .join(Tables.DENIZEN.name)
                .find(ProjectRecord().apply {
                    courseId = query.find?.getInteger(Tables.PROJECT.COURSE_ID.name)
                    denizenId = query.find?.getInteger(Tables.PROJECT.DENIZEN_ID.name)
                })

        val qWithSearch = if (query.text == "") q else q.filter("document matches %s".formatToQuery(query.text))

        return sendJsonableAsync(Address.DB.count(Tables.PROJECT.name), qWithSearch)
    }
}
