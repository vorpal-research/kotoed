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
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun verify(data: JsonObject?): VerificationData {
        val wc = WebClient.create(vertx)

        val projectRecord: ProjectRecord = data?.toRecord()
                ?: throw IllegalArgumentException("Cannot verify $data")

        val schedulerLocator = DimensionLocator(
                "forceschedulers",
                Kotoed2Buildbot.projectName2schedulerName(projectRecord.name))

        val response = tryOrNull {
            wc.head(Config.Buildbot.Port, Config.Buildbot.Host, BuildbotApi.Root + schedulerLocator)
                    .putDefaultBBHeaders()
                    .sendAsync()
        }

        if (response != null
                && HttpResponseStatus.OK.code() == response.statusCode()) {
            return VerificationData.Processed

        } else {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject(
                                "failure" to "Buildbot scheduler for ${projectRecord.name} not available",
                                "details" to (response?.errorDetails ?: "Buildbot is down?")
                        )
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

            res.ignore()

            return VerificationData.Processed

        } catch (ex: Exception) {
            val error = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject(
                                "failure" to "Exception when processing ${projectRecord.toJson()}",
                                "details" to "$ex"
                        )
                    }

            val errorId = dbCreateAsync(error).id

            return VerificationData.Invalid(errorId)
        }
    }

}
