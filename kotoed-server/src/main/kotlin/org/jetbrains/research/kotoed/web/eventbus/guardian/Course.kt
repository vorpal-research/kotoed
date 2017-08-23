package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.set
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher
import org.jetbrains.research.kotoed.web.eventbus.patchers.logPatch
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

class CourseListPatcher(val vertx: Vertx) : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val user = be.socket().webUser()
        val isTeacher = user?.isAuthorisedAsync(Authority.Teacher) ?: false

        if (isTeacher)
            return

        val rawMessage = be.rawMessage
        val body = rawMessage["body"] as? JsonObject ?: return

        body["find"] = (body["find"] as? JsonObject) ?: JsonObject()

        (body["find"] as JsonObject)["denizen_id"] = user.principal()["id"]
        rawMessage["body"] = body

        be.rawMessage = rawMessage
        logPatch(be)
    }

    override fun toString(): String {
        return "CourseListPatcher(vertx=$vertx)"
    }
}