package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.scope
import org.jetbrains.research.kotoed.web.auth.isProjectOwner
import org.jetbrains.research.kotoed.web.auth.isSubmissionOwner
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

/* Sum stuff */

private suspend fun BridgeEvent.isSubmissionReady(vertx: Vertx, id: Int): Boolean {
    val sub = vertx.scope.submissionByIdOrNull(id) ?: return false
    return when (sub.state) {
        SubmissionState.pending,
        SubmissionState.invalid -> false
        else -> true
    }
}

class SubmissionReady(
        val vertx: Vertx,
        private val submissionIdPath: String = "submission_id") : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(submissionIdPath)
                ?: return false

        return be.isSubmissionReady(vertx, id)
    }

    override fun toString(): String {
        return "SubmissionReady(vertx=$vertx)"
    }
}

private suspend fun BridgeEvent.isSubmissionOpen(vertx: Vertx, id: Int): Boolean {
    val sub = vertx.scope.submissionByIdOrNull(id) ?: return false
    return SubmissionState.open == sub.state
}

class SubmissionOpen(
        val vertx: Vertx,
        private val submissionIdPath: String = "submission_id") : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(submissionIdPath)
                ?: return false

        return be.isSubmissionOpen(vertx, id)
    }

    override fun toString(): String {
        return "SubmissionOpen(vertx=$vertx)"
    }
}

/* Ownership stuff */

private suspend fun BridgeEvent.checkProjectOwnership(vertx: Vertx, id: Int): Boolean {
    return this.socket()?.webUser()?.isProjectOwner(vertx, id) ?: false
}

private suspend fun BridgeEvent.checkSubmissionOwnership(vertx: Vertx, id: Int): Boolean {
    return this.socket()?.webUser()?.isSubmissionOwner(vertx, id) ?: false
}

class ShouldBeProjectOwnerForFilter(
        val vertx: Vertx,
        private val projectIdPath: String = "project_id"
) : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getJsonObject("find")
                ?.getInteger(projectIdPath)
                ?: return false

        return be.checkProjectOwnership(vertx, id)
    }

    override fun toString(): String {
        return "ShouldBeProjectOwnerForFilter(vertx=$vertx)"
    }
}

class ShouldBeProjectOwner(
        val vertx: Vertx,
        private val projectIdPath: String = "project_id"
) : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(projectIdPath)
                ?: return false

        return be.checkProjectOwnership(vertx, id)
    }

    override fun toString(): String {
        return "ShouldBeProjectOwner(vertx=$vertx)"
    }
}

class ShouldBeSubmissionOwnerForFilter(
        val vertx: Vertx,
        private val submissionIdPath: String = "submission_id"
) : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getJsonObject("find")
                ?.getInteger(submissionIdPath)
                ?: return false

        return be.checkSubmissionOwnership(vertx, id)
    }

    override fun toString(): String {
        return "ShouldBeSubmissionOwnerForFilter(vertx=$vertx)"
    }
}

class ShouldBeSubmissionOwner(
        val vertx: Vertx,
        private val submissionIdPath: String = "submission_id"
) : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(submissionIdPath)
                ?: return false
        
        return be.checkSubmissionOwnership(vertx, id)
    }

    override fun toString(): String {
        return "ShouldBeSubmissionOwner(vertx=$vertx)"
    }
}
