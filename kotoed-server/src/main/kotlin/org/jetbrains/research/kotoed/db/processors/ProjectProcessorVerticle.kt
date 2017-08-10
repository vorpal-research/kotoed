package org.jetbrains.research.kotoed.db.processors

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.buildbot.util.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.buildbot.project.CreateProject
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun verify(data: JsonObject?): VerificationData {
        val eb = vertx.eventBus()

        val wc = WebClient.create(vertx)

        val record: ProjectRecord = data?.toRecord() ?: throw IllegalArgumentException("Cannot verify $data")

        val schedulerLocator = DimensionLocator(
                "forceschedulers",
                Kotoed2Buildbot.projectName2schedulerName(record.name))

        // TODO: gracefully fail when buildbot is  unavailable

        val response = vxa<HttpResponse<Buffer>> {
            wc.head(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Root + schedulerLocator)
                    .putDefaultBBHeaders()
                    .send(it)
        }

        if (HttpResponseStatus.OK.code() == response.statusCode()) {
            return VerificationData.Processed

        } else {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = record.id
                        this.data = JsonObject("error" to "Buildbot scheduler for ${record.name} not available")
                    }

            val errorId = vxa<Message<JsonObject>> {
                eb.send(Address.DB.create(Tables.PROJECT_STATUS.name), error.toJson(), it)
            }.body().toRecord<ProjectStatusRecord>().id

            return VerificationData.Invalid(errorId)
        }
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {

        val eb = vertx.eventBus()

        val projectRecord: ProjectRecord = data.toRecord()

        val courseRecord = db {
            with(Tables.COURSE) {
                select(NAME)
                        .from(this)
                        .where(ID.eq(projectRecord.courseId))
                        .fetchOne()
                        .into(CourseRecord::class.java)
            }
        }

        val createProject = CreateProject(
                projectRecord.id,
                courseRecord.name,
                projectRecord.name,
                projectRecord.repoUrl,
                projectRecord.repoType
        )

        try {
            vxa<Message<JsonObject>> {
                eb.send(
                        Address.Buildbot.Project.Create,
                        createProject.toJson(),
                        it
                )
            }

            return VerificationData.Processed

        } catch (ex: Exception) {
            val er = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject("error" to "Error in Buildbot: ${ex.message}")
                    }

            val erId = vxa<Message<JsonObject>> {
                eb.send(Address.DB.create(Tables.PROJECT_STATUS.name), er.toJson(), it)
            }.body().toRecord<ProjectStatusRecord>().id

            return VerificationData.Invalid(erId)
        }
    }

}
