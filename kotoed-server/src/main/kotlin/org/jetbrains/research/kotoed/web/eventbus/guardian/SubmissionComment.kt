package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.scope
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.util.withVertx
import org.jetbrains.research.kotoed.web.eventbus.commentByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

object CommentCreatePatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val authorId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val body = rawMessage.getJsonObject("body") ?: return

        body["authorId"] = authorId
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "CommentCreatePatcher"
    }
}

private suspend fun BridgeEvent.isCommentSubmissionOpen(vertx: Vertx, id: Int): Boolean = withVertx(vertx) {
    val comment = commentByIdOrNull(id) ?: return false
    val submission = submissionByIdOrNull(comment.submissionId) ?: return false
    return SubmissionState.open == submission.state
}

class CommentSubmissionOpen(
        val vertx: Vertx,
        private val commentIdPath: String = "id") : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(commentIdPath)
                ?: return false

        return be.isCommentSubmissionOpen(vertx, id)
    }

    override fun toString(): String {
        return "CommentSubmissionOpen(vertx=$vertx)"
    }
}

private suspend fun BridgeEvent.checkCommentOwnership(vertx: Vertx, id: Int): Boolean {
    val comment = vertx.scope.commentByIdOrNull(id) ?: return false
    return comment.authorId == this.socket()?.webUser()?.principal()?.getInteger("id")
}

class ShouldBeCommentOwner(
        val vertx: Vertx,
        private val commentIdPath: String = "id") : LoggingBridgeEventFilter() {
    suspend override fun checkIsAllowed(be: BridgeEvent): Boolean {
        val id = be.rawMessage
                ?.getJsonObject("body")
                ?.getInteger(commentIdPath)
                ?: return false

        return be.checkCommentOwnership(vertx, id)
    }

    override fun toString(): String {
        return "ShouldBeCommentOwner(vertx=$vertx)"
    }
}
