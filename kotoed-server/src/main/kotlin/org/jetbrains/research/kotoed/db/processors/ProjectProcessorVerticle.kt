package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.teamcity.project.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.*
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun checkPrereqs(data: JsonObject): List<VerificationData> {

        val eb = vertx.eventBus()

        val prereqs = listOf(Tables.COURSE to data[Tables.PROJECT.COURSE_ID])

        return prereqs
                .map { (table, id) ->
                    vxa<Message<JsonObject>> {
                        eb.send(
                                Address.DB.verify(table.name),
                                JsonObject("id" to id),
                                it
                        )
                    }
                }
                .map { it.body().toJsonable<VerificationData>() }
    }

    suspend override fun verify(data: JsonObject?): VerificationData {

        val eb = vertx.eventBus()

        val projectRecord: ProjectRecord = data?.toRecord() ?: throw IllegalArgumentException("Cannot verify $data")

        val tcQueries = listOf(
                DimensionQuery(
                        TeamCityApi.Projects,
                        DimensionLocator("id", name2id(projectRecord.name))
                ),
                DimensionQuery(
                        TeamCityApi.VcsRoots,
                        DimensionLocator("id", name2vcs(projectRecord.name))
                ),
                DimensionQuery(
                        TeamCityApi.BuildTypes,
                        DimensionLocator("id", name2build(projectRecord.name))
                )
        )

        val res = tcQueries.map { q ->
            vxa<Message<JsonObject>> {
                eb.send(
                        Address.TeamCity.Proxy,
                        q.toJson(),
                        it
                )
            }
        }.map { it.body() }

        val errorIds = res.zip(tcQueries)
                .filter { it.first.isEmpty }
                .map { (_, query) ->
                    val er = ProjectStatusRecord()
                            .apply {
                                this.projectId = projectRecord.id
                                this.data = JsonObject("error" to "Cannot find $query on TeamCity")
                            }

                    vxa<Message<JsonObject>> {
                        eb.send(Address.DB.create(Tables.PROJECT_STATUS.name), er.toJson(), it)
                    }.body().toRecord<ProjectStatusRecord>().id
                }

        return VerificationData(errorIds)
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {

        val eb = vertx.eventBus()

        val projectRecord: ProjectRecord = data.toRecord()

        val courseRecord = db {
            with(Tables.COURSE) {
                select(ROOT_PROJECT_ID, BUILD_TEMPLATE_ID)
                        .from(this)
                        .where(ID.eq(projectRecord.courseId))
                        .fetchOne()
                        .into(CourseRecord::class.java)
            }
        }

        val createProject = CreateProject(
                Project(
                        name2id(projectRecord.name),
                        projectRecord.name,
                        courseRecord.rootProjectId
                ),
                VcsRoot(
                        name2vcs(projectRecord.name),
                        name2vcs(projectRecord.name),
                        projectRecord.repoType,
                        projectRecord.repoUrl,
                        name2id(projectRecord.name)
                ),
                BuildConfig(
                        name2build(projectRecord.name),
                        name2build(projectRecord.name),
                        courseRecord.buildTemplateId
                )
        )

        try {
            vxa<Message<JsonObject>> {
                eb.send(
                        Address.TeamCity.Project.Create,
                        createProject.toJson(),
                        it
                )
            }
        } catch (ex: Exception) {
            val er = ProjectStatusRecord()
                    .apply {
                        this.projectId = projectRecord.id
                        this.data = JsonObject("error" to "Error in TeamCity: ${ex.message}")
                    }

            val erId = vxa<Message<JsonObject>> {
                eb.send(Address.DB.create(Tables.PROJECT_STATUS.name), er.toJson(), it)
            }.body().toRecord<ProjectStatusRecord>().id

            return VerificationData.Invalid(erId)
        }

        return VerificationData.Processed
    }

}