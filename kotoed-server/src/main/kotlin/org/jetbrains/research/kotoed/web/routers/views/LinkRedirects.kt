package org.jetbrains.research.kotoed.web.routers.views

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.NotFound
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.redirect
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.SubmissionResults.ById)
@LoginRequired
suspend fun handleSubmissionResultsById(context: RoutingContext) {
    val id by context.request()

    id ?: throw NotFound("id is null")

    context.response().redirect(UrlPattern.reverse(
            UrlPattern.Submission.Results,
            mapOf("id" to id)
    ))
}
