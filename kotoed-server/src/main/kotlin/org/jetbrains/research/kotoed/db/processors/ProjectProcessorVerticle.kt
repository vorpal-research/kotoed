package org.jetbrains.research.kotoed.db.processors

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
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
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.sendAsync
import org.jetbrains.research.kotoed.util.sendJsonableAsync

@AutoDeployable
class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun verify(data: JsonObject?): VerificationData {
        val wc = WebClient.create(vertx)

        val projectRecord: ProjectRecord = data?.toRecord()
                ?: throw IllegalArgumentException("Cannot verify $data")

        val schedulerLocator = DimensionLocator(
                "forceschedulers",
                Kotoed2Buildbot.projectName2schedulerName(projectRecord.name))

        val response = try {
            wc.head(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Root + schedulerLocator)
                    .putDefaultBBHeaders()
                    .sendAsync()

        } catch (ex: Exception) {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject("error" to
                                "Exception when verifying ${projectRecord.toJson()}: $ex")
                    }

            val errorId = dbCreateAsync(error).id

            return VerificationData.Invalid(errorId)
        }

        if (HttpResponseStatus.OK.code() == response.statusCode()) {
            return VerificationData.Processed

        } else {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject("error" to
                                "Buildbot scheduler for ${projectRecord.name} not available: ${response.statusMessage()}/${response.bodyAsString()}")
                    }

            val errorId = dbCreateAsync(error).id

            return VerificationData.Invalid(errorId)
        }
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {

        val projectRecord: ProjectRecord = data.toRecord()

        val courseRecord = dbFetchAsync(
                CourseRecord().setId(projectRecord.courseId))

        val createProject = CreateProject(
                projectRecord.id,
                courseRecord.name,
                projectRecord.name,
                projectRecord.repoUrl,
                projectRecord.repoType
        )

        try {
            val res: JsonObject = sendJsonableAsync(
                    Address.Buildbot.Project.Create,
                    createProject
            )

            return VerificationData.Processed

        } catch (ex: Exception) {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject("error" to
                                "Exception when processing ${projectRecord.toJson()}: $ex")
                    }

            val errorId = dbCreateAsync(error).id

            return VerificationData.Invalid(errorId)
        }
    }

}
