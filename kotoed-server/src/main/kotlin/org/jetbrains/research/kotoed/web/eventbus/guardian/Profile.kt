package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

class AddUsernamePatcher(private val path: String = "denizen_id") : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val authorId = be.socket().webUser().principal().getString("denizenId")
        val rawMessage = be.rawMessage
        val body = rawMessage.getJsonObject("body") ?: return

        body[path] = authorId
        rawMessage["body"] = body

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "AddUsernamePatcher()"
    }
}