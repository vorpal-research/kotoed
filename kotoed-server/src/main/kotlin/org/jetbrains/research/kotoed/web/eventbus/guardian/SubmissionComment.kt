package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.isAuthorised
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.commentById
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher
import org.jetbrains.research.kotoed.web.eventbus.submissionById

object CommentCreatePatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val authorId = be.socket().webUser().principal()["id"]
        val rawMessage = be.rawMessage
        val body = rawMessage["body"] as? JsonObject ?: return

        body["authorId"] = authorId
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "CommentCreatePatcher"
    }
}

class CommentCreateFilter(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger("submission_id") ?: return@run false
        val submission = vertx.eventBus().submissionById(id) ?: return false

        return@run submission.state == SubmissionState.open
    }.also { logResult(be, it) }

    override fun toString(): String {
        return "CommentCreateFilter(vertx=$vertx)"
    }
}

class CommentUpdateFilter(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val user = be.socket().webUser()
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger("id") ?: return@run false

        val comment = vertx.eventBus().commentById(id) ?: return@run false

        val submission = vertx.eventBus().submissionById(comment.submissionId) ?: return@run false

        if (submission.state != SubmissionState.open)
            return@run false

        return@run if (comment.authorId != user.principal()["id"])
             true
        else
            user.isAuthorised("teacher")

    }.also {
        logResult(be, it)
    }

    override fun toString(): String {
        return "CommentUpdateFilter(vertx=$vertx)"
    }


}
