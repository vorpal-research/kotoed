package org.jetbrains.research.kotoed.api

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Project.Create)
    suspend fun handleCreate(project: ProjectRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

        val res: ProjectRecord = vxa<Message<JsonObject>> {
            eb.send(Address.DB.create(project.table.name), project.toJson(), it)
        }.body().toRecord()

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
    suspend fun handleError(verificationData: VerificationData): JsonObject =
            verificationData.errors
                    .map { selectById(Tables.PROJECT_STATUS, it, ProjectStatusRecord::class) }
                    .map { it.toJson() }
                    .let { JsonObject("data" to it) }

}
