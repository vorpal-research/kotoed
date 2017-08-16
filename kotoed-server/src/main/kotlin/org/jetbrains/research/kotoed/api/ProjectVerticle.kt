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
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Project.Create)
    suspend fun handleCreate(project: ProjectRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

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
                .filter("""project.document matches "${query.text}"""")
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
                .filter("""project.document matches "${query.text}"""")

        return sendJsonableAsync(Address.DB.count("submission"), q)
    }

}
