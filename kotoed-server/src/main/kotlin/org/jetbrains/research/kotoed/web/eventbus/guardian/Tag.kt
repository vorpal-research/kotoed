package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

object WithTagsPatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val rawMessage = be.rawMessage
        val body = rawMessage["body"] as? JsonObject ?: return

        body["with_tags"] = be.socket().webUser().isAuthorisedAsync(Authority.Teacher)
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "CommentCreatePatcher"
    }
}
