package org.jetbrains.research.kotoed.web.routers

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern

@HandlerFor(UrlPattern.Auth.ResetPassword)
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
suspend fun resetPasswordEndpointHandler(context: RoutingContext) {
    val body: DenizenRecord = context.request().bodyAsync().toJsonObject().toRecord()

    val search: List<DenizenRecord> =
            context.vertx().eventBus().sendJsonableCollectAsync(
                    Address.DB.find("denizen"),
                    DenizenRecord().apply{ denizenId = body.denizenId!!; email = body.email!! }
            )

    if(search.isEmpty()) throw Unauthorized("Incorrect email-login combination")

    run<Unit> { context.vertx().eventBus().sendJsonableAsync(Address.User.Auth.Restore, search.first()) }
    context.jsonResponse().end(JsonObject("succeeded" to true))
}

@HandlerFor(UrlPattern.Auth.ResetPassword)
@ForHttpMethod(HttpMethod.GET)
@Templatize("resetPassword.jade")
@JsBundle("resetPassword")
suspend fun resetPasswordPageHandler(context: RoutingContext) {}
