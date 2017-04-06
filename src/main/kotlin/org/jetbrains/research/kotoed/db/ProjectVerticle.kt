package org.jetbrains.research.kotoed.db

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.db.ProjectsForCourse
import org.jetbrains.research.kotoed.database.Tables.COURSE
import org.jetbrains.research.kotoed.database.Tables.PROJECT
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.jooq
import org.jetbrains.research.kotoed.util.fromJson
import org.jetbrains.research.kotoed.util.ignore
import org.jetbrains.research.kotoed.util.withExceptions

class ProjectVerticle : DatabaseVerticle<ProjectRecord>(PROJECT) {
    val projectsForCourseAddress = "kotoed.$entityName.for.course"

    override fun start() {
        super.start()

        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                projectsForCourseAddress,
                this::handleProjectsForCourse.withExceptions()
        )
    }

    fun handleProjectsForCourse(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val projectsForCourse = fromJson<ProjectsForCourse>(msg.body())

        jooq(dataSource).use {
            val res =
                    it.selectFrom(PROJECT.join(COURSE).onKey())
                            .where(COURSE.ID.eq(projectsForCourse.courseId))
                            .fetchKAsync()
                            .into(JsonObject::class.java)
                            .let(::JsonArray)

            msg.reply(res)
        }
    }.ignore()
}
