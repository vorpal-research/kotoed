package org.jetbrains.research.kotoed.db.processors

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.DimensionLocator
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.teamcity.util.plus
import org.jetbrains.research.kotoed.teamcity.util.putDefaultTCHeaders
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.vxa

@AutoDeployable
class CourseProcessorVerticle : ProcessorVerticle<CourseRecord>(Tables.COURSE) {

    suspend override fun verify(data: JsonObject?): VerificationData {
        val eb = vertx.eventBus()

        val wc = WebClient.create(vertx)

        val record: CourseRecord = data?.toRecord() ?: throw IllegalArgumentException("Cannot verify $data")

        val buildTypeUrl = TeamCityApi.BuildTypes +
                DimensionLocator.from("id", record.buildTemplateId)
        val buildTypeRes = vxa<HttpResponse<Buffer>> {
            wc.get(Config.TeamCity.Port, Config.TeamCity.Host, buildTypeUrl)
                    .putDefaultTCHeaders()
                    .send(it)
        }
        val hasTemplate = if (HttpResponseStatus.OK.code() == buildTypeRes.statusCode()) {
            buildTypeRes.bodyAsJsonObject().getBoolean("templateFlag", false)
        } else false

        val rootProjectUrl = TeamCityApi.Projects +
                DimensionLocator.from("id", record.rootProjectId)
        val rootProjectRes = vxa<HttpResponse<Buffer>> {
            wc.get(Config.TeamCity.Port, Config.TeamCity.Host, rootProjectUrl)
                    .putDefaultTCHeaders()
                    .send(it)
        }
        val hasRootProject = HttpResponseStatus.OK.code() == rootProjectRes.statusCode()

        val errorRecords = mutableListOf<CourseStatusRecord>()

        if (!hasRootProject) { // No root project

            errorRecords += CourseStatusRecord()
                    .apply {
                        this.courseId = record.id
                        this.data = JsonObject("error" to "No root project <${record.rootProjectId}>")
                    }

        }

        if (!hasTemplate) { // No build template

            errorRecords += CourseStatusRecord()
                    .apply {
                        this.courseId = record.id
                        this.data = JsonObject("error" to "No build template <${record.buildTemplateId}>")
                    }

        }

        val errorIds = errorRecords.map { er ->
            vxa<Message<JsonObject>> {
                eb.send(Address.DB.create(Tables.COURSE_STATUS.name), er.toJson(), it)
            }.body().toRecord<CourseStatusRecord>().id
        }

        return VerificationData(errorIds)
    }

}