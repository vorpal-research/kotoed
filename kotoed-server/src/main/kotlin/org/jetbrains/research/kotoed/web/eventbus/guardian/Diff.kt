package org.jetbrains.research.kotoed.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.data.api.Code
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.util.scope
import org.jetbrains.research.kotoed.web.auth.isProjectOwner
import org.jetbrains.research.kotoed.web.auth.isSubmissionOwner
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull

class ShouldNotRequestLastChecked(
        val vertx: Vertx,
) : LoggingBridgeEventFilter() {
    override suspend fun checkIsAllowed(be: BridgeEvent): Boolean {
        val diffType = be.rawMessage
                ?.getJsonObject("body")
                ?.getJsonObject("base")
                ?.getString("type")
                ?: return false
        
        return diffType != Code.Submission.DiffBaseType.PREVIOUS_CHECKED.toString();
    }

    override fun toString(): String {
        return "ShouldBeSubmissionOwner(vertx=$vertx)"
    }
}
