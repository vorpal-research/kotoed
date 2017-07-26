package org.jetbrains.research.kotoed.db.processors

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.buildbot.util.BuildbotApi
import org.jetbrains.research.kotoed.buildbot.util.Kotoed2Buildbot
import org.jetbrains.research.kotoed.buildbot.util.StringLocator
import org.jetbrains.research.kotoed.buildbot.util.putDefaultBBHeaders
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.vxa

@AutoDeployable
class CourseProcessorVerticle : ProcessorVerticle<CourseRecord>(Tables.COURSE) {

    suspend override fun doProcess(data: JsonObject): VerificationData =
            verify(data)

    suspend override fun verify(data: JsonObject?): VerificationData {
        val eb = vertx.eventBus()

        val wc = WebClient.create(vertx)

        val record: CourseRecord = data?.toRecord() ?: throw IllegalArgumentException("Cannot verify $data")

        val endpointLocator = StringLocator(
                Kotoed2Buildbot.courseName2endpoint(record.name))

        val response = vxa<HttpResponse<Buffer>> {
            wc.head(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Empty + endpointLocator)
                    .putDefaultBBHeaders()
                    .send(it)
        }

        if (HttpResponseStatus.OK.code() == response.statusCode()) {
            return VerificationData.Processed

        } else {
            val error = CourseStatusRecord()
                    .apply {
                        this.courseId = record.id
                        this.data = JsonObject("error" to "Buildbot endpoint $endpointLocator not available")
                    }

            val errorId = vxa<Message<JsonObject>> {
                eb.send(Address.DB.create(Tables.COURSE_STATUS.name), error.toJson(), it)
            }.body().toRecord<CourseStatusRecord>().id

            return VerificationData.Invalid(errorId)
        }

    }
}
