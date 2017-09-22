package org.jetbrains.research.kotoed.web.auth

import io.vertx.core.Vertx
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.web.eventbus.projectByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

suspend fun User.isProjectOwner(project: ProjectRecord): Boolean {
    return project.denizenId == this.principal()?.getInteger("id")
}

suspend fun User.isProjectOwner(vertx: Vertx, id: Int): Boolean {
    return this.isProjectOwner(vertx.eventBus().projectByIdOrNull(id) ?: return false)
}

suspend fun User.isSubmissionOwner(vertx: Vertx, id: Int): Boolean {
    val sub = vertx.eventBus().submissionByIdOrNull(id) ?: return false
    return this.isProjectOwner(vertx, sub.projectId)
}

suspend fun User.isProjectOwnerOrTeacher(project: ProjectRecord) =
        isProjectOwner(project) || isAuthorisedAsync(Authority.Teacher)
