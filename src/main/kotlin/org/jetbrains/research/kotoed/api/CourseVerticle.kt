package org.jetbrains.research.kotoed.api

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class CourseVerticle : AbstractKotoedVerticle(), Loggable {

    @JsonableEventBusConsumerFor(Address.Api.Course.Create)
    suspend fun handleCreate(course: CourseRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

        val res: CourseRecord = vxa<Message<JsonObject>> {
            eb.send(Address.DB.create(course.table.name), course.toJson(), it)
        }.body().toRecord()

        eb.send(Address.DB.process(course.table.name), res.toJson())

        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Read)
    suspend fun handleRead(course: CourseRecord): DbRecordWrapper {
        val eb = vertx.eventBus()

        val res: CourseRecord = vxa<Message<JsonObject>> {
            eb.send(Address.DB.read(course.table.name), course.toJson(), it)
        }.body().toRecord()

        val status: VerificationData = vxa<Message<JsonObject>> {
            eb.send(Address.DB.process(course.table.name), res.toJson(), it)
        }.body().toJsonable()

        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Error)
    suspend fun handleError(verificationData: VerificationData): List<CourseStatusRecord> =
            verificationData.errors
                    .map { fetchByIdAsync(Tables.COURSE_STATUS, it) }

}
