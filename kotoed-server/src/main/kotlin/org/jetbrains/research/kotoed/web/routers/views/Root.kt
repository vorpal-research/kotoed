package org.jetbrains.research.kotoed.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.util.routing.Templatize

@HandlerFor("/views/submission/:id/results")
@Templatize("submissionResults.jade")
@LoginRequired
@JsBundle("submissionResults")
suspend fun handleSubmissionResults(context: RoutingContext) {
    val id by context.request()
    val id_ = id?.toInt() ?: run {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }
}
