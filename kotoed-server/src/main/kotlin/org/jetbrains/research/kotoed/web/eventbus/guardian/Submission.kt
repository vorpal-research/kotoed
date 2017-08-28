package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult
import org.jetbrains.research.kotoed.web.eventbus.projectByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

class SubmissionReady(val vertx: Vertx, val submissionIdParamName: String = "id") : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger(submissionIdParamName) ?: return@run false
        val submission = vertx.eventBus().submissionByIdOrNull(id) ?: return@run false

        return@run submission.state != SubmissionState.pending && submission.state != SubmissionState.invalid
    }.also { logResult(be, it) }

    override fun toString(): String {
        return "SubmissionReady(vertx=$vertx)"
    }
}

private suspend fun BridgeEvent.checkProjectOwnership(vertx: Vertx, id: Int): Boolean {
    val project = vertx.eventBus().projectByIdOrNull(id) ?: return false
    return project.denizenId == this.socket()?.webUser()?.principal()?.getInteger("id")
}


class ShouldBeProjectOwnerForFilter(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val body = be.rawMessage?.get("body") as? JsonObject
        val find = body?.get("find") as? JsonObject
        val id = find?.getInteger("project_id") ?: return@run false

        return@run be.checkProjectOwnership(vertx, id)
    }.also { logResult(be, it) }

    override fun toString(): String {
        return "ShouldBeProjectOwnerForFilter(vertx=$vertx)"
    }
}

class ShouldBeProjectOwner(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger("project_id") ?: return@run false
        return@run be.checkProjectOwnership(vertx, id)
    }.also { logResult(be, it) }

    override fun toString(): String {
        return "ShouldBeProjectOwner(vertx=$vertx)"
    }
}
