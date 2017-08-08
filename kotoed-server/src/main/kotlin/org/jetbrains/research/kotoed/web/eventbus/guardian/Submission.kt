package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotoed.web.eventbus.filters.logResult
import org.jetbrains.research.kotoed.web.eventbus.submissionById

class SubmissionReady(val vertx: Vertx, val submissionIdParamName: String = "id") : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean = run {
        val id = (be.rawMessage?.get("body") as? JsonObject)?.getInteger(submissionIdParamName) ?: return@run false
        val submission = vertx.eventBus().submissionById(id) ?: return@run false

        return submission.state != SubmissionState.pending && submission.state != SubmissionState.invalid
    }.also { logResult(be, it) }

    override fun toString(): String {
        return "SubmissionReady(vertx=$vertx)"
    }


}