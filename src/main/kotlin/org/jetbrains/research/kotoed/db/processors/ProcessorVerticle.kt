package org.jetbrains.research.kotoed.db.processors

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.teamcity.project.BuildConfig
import org.jetbrains.research.kotoed.data.teamcity.project.CreateProject
import org.jetbrains.research.kotoed.data.teamcity.project.Project
import org.jetbrains.research.kotoed.data.teamcity.project.VcsRoot
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.db.DatabaseVerticle
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.*
import org.jetbrains.research.kotoed.util.*
import org.jooq.Table
import org.jooq.UpdatableRecord
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

enum class VerificationStatus {
    Unknown,
    NotReady,
    Processed,
    Invalid
}

data class VerificationData(
        val id: Int,
        val status: VerificationStatus) : Jsonable

abstract class ProcessorVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val processAddress = Address.DB.process(entityName)
    val verifyAddress = Address.DB.verify(entityName)

    val cache: Cache<Int, VerificationStatus> = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES) // TODO: Move to settings
            .build<Int, VerificationStatus>()

    val cacheMap: ConcurrentMap<Int, VerificationStatus>
        get() = cache.asMap()

    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                processAddress,
                this::handleProcess
        )

        eb.consumer<JsonObject>(
                verifyAddress,
                this::handleVerify
        )
    }

    fun handleProcess(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        val data = db { selectById(id) }
        val oldStatus = cacheMap.putIfAbsent(id, VerificationStatus.Unknown)
        if (VerificationStatus.Unknown == oldStatus) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        process(data)
    }.ignore()

    fun handleVerify(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        val data = db { selectById(id) }
        val oldStatus = cacheMap.putIfAbsent(id, VerificationStatus.Unknown)
        if (VerificationStatus.Unknown == oldStatus) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        msg.reply(VerificationData(id, cache[id] ?: VerificationStatus.Unknown).toJson())
    }.ignore()

    suspend fun process(data: JsonObject?) {
        data ?: throw IllegalArgumentException("data is null")

        val id = data[pk.name] as Int

        val oldStatus = cache[id] ?: VerificationStatus.Unknown

        when (oldStatus) {
            VerificationStatus.Processed, VerificationStatus.NotReady -> {
                // do nothing
            }
            else -> {
                val ok = cacheMap.replace(id, oldStatus, VerificationStatus.NotReady)
                if (ok) {

                    val statuses = checkPrereqs(data)

                    if (statuses.all { VerificationStatus.Processed == it }) {

                        doProcess(data)

                    } else if (statuses.any { VerificationStatus.Invalid == it }) {

                        // do nothing

                    } else {

                        // do nothing?
                        // retry later?

                    }
                } else { // retry
                    process(data)
                }
            }
        }
    }

    suspend open fun verify(data: JsonObject?): VerificationStatus =
            VerificationStatus.Processed

    suspend open fun doProcess(data: JsonObject) {
    }

    suspend open fun checkPrereqs(data: JsonObject): List<VerificationStatus> =
            emptyList()

}

class CourseProcessorVerticle : ProcessorVerticle<CourseRecord>(Tables.COURSE) {

    suspend override fun verify(data: JsonObject?): VerificationStatus {
        val wc = WebClient.create(vertx)

        data ?: return VerificationStatus.Invalid
        // FIXME akhin Write event info to DB

        val buildtemplateid: String by data.delegate

        val url = TeamCityApi.BuildTypes +
                DimensionLocator.from("id", buildtemplateid)

        val res = vxa<HttpResponse<Buffer>> {
            wc.get(Config.TeamCity.Port, Config.TeamCity.Host, url)
                    .putDefaultTCHeaders()
                    .send(it)
        }

        if (HttpResponseStatus.OK.code() == res.statusCode()) {
            val baj = res.bodyAsJsonObject()

            val isTemplate = baj.getBoolean("templateFlag", false)

            if (isTemplate) return VerificationStatus.Processed
        }

        // FIXME akhin Write event info to DB

        return VerificationStatus.Invalid
    }

}

class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun checkPrereqs(data: JsonObject): List<VerificationStatus> {
        val eb = vertx.eventBus()

        val prereqs = listOf(Tables.COURSE to data[Tables.PROJECT.COURSEID])

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
                .map { fromJson<VerificationData>(it.body()) }
                .map { it.status }
    }

    suspend override fun doProcess(data: JsonObject) {
        val eb = vertx.eventBus()

        val projectName = data[Tables.PROJECT.NAME] ?: throw IllegalArgumentException("No project.name found in: $data")
        val courseId = data[Tables.PROJECT.COURSEID] ?: throw IllegalArgumentException("No project.courseid found in: $data")

        val templateId = db {
            select(Tables.COURSE.BUILDTEMPLATEID)
                    .from(Tables.COURSE)
                    .where(Tables.COURSE.ID.eq(courseId))
                    .fetchOne()
                    .into(String::class.java)
        }

        val createProject = CreateProject(
                Project(
                        name2id(projectName),
                        projectName,
                        "_Root" // FIXME
                ),
                VcsRoot(
                        name2vcs(projectName),
                        name2vcs(projectName),
                        data[Tables.PROJECT.REPOTYPE] ?: throw IllegalArgumentException("No project.repotype found in: $data"),
                        data[Tables.PROJECT.REPOURL] ?: throw IllegalArgumentException("No project.repourl found in: $data"),
                        name2id(projectName)
                ),
                BuildConfig(
                        name2build(projectName),
                        name2build(projectName),
                        templateId
                )
        )

        vxa<Message<JsonObject>> {
            eb.send(
                    Address.TeamCity.Project.Create,
                    createProject,
                    it
            )
        }
    }

}
