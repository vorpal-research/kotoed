package org.jetbrains.research.kotoed.db.processors

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.api.bang
import org.jetbrains.research.kotoed.data.teamcity.project.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseStatusRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectStatusRecord
import org.jetbrains.research.kotoed.db.DatabaseVerticle
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.*
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Table
import org.jooq.UpdatableRecord
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

abstract class ProcessorVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val processAddress = Address.DB.process(entityName)
    val verifyAddress = Address.DB.verify(entityName)

    val cache: Cache<Int, VerificationData> = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES) // TODO: Move to settings
            .build()

    val cacheMap: ConcurrentMap<Int, VerificationData>
        get() = cache.asMap()

    override fun start(startFuture: Future<Void>) {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(processAddress, this::handleProcess)
        eb.consumer<JsonObject>(verifyAddress, this::handleVerify)

        super.start(startFuture)
    }

    fun handleProcess(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        val data = db { selectById(id) }
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        process(data)
        msg.reply(cache[id].bang().toJson())
    }.ignore()

    fun handleVerify(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        val data = db { selectById(id) }
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        msg.reply(cache[id].bang().toJson())
    }.ignore()

    suspend fun process(data: JsonObject?) {
        data ?: throw IllegalArgumentException("data is null")

        val id = data[pk.name] as Int

        val oldData = cache[id].bang()

        when (oldData.status) {
            VerificationStatus.Processed, VerificationStatus.NotReady -> {
                // do nothing
            }
            else -> {

                val notReady = oldData.copy(VerificationStatus.NotReady)

                val ok = cacheMap.replace(id, oldData, notReady)

                if (ok) {

                    val prereqVerificationData = checkPrereqs(data)

                    if (prereqVerificationData.all { VerificationStatus.Processed == it.status }) {

                        cacheMap.replace(id, notReady, doProcess(data))

                    } else if (prereqVerificationData.any { VerificationStatus.Invalid == it.status }) {

                        cacheMap.replace(id, notReady,
                                prereqVerificationData
                                        .filter { VerificationStatus.Invalid == it.status }
                                        .reduce { acc, data -> acc.copy(errors = acc.errors + data.errors) }
                        )

                    } else {

                        cacheMap.replace(id, notReady, VerificationData.Unknown)

                    }
                } else { // retry
                    process(data)
                }
            }
        }
    }

    suspend open fun verify(data: JsonObject?): VerificationData =
            VerificationData.Processed

    suspend open fun doProcess(data: JsonObject): VerificationData =
            VerificationData.Processed

    suspend open fun checkPrereqs(data: JsonObject): List<VerificationData> =
            emptyList()

}

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
