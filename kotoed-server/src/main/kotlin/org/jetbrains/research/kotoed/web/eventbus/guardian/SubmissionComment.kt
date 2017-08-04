package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.database.tables.records.SubmissionCommentRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

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

object CommentCreateFilter : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = true.also { logResult(be, it) }

    override fun toString(): String {
        return "CommentCreateFilter"
    }
}

class CommentUpdateFilter(val vertx: Vertx) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val user = be.socket().webUser()
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger("id") ?: return@run false

        val commentWrapper = fromJson<DbRecordWrapper>(vertx.eventBus().sendAsync(
                Address.Api.Submission.Comment.Read,
                SubmissionCommentRecord().apply { this.id = id }.toJson()).body())

        val comment = fromJson<SubmissionCommentRecord>(commentWrapper.record)

        return@run if (comment.authorId == user.principal()["id"])
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
