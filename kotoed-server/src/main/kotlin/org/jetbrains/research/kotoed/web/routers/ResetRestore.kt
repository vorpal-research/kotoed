package org.jetbrains.research.kotoed.web.routers

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import java.util.*

@HandlerFor(UrlPattern.Auth.ResetPassword)
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
suspend fun resetPasswordEndpointHandler(context: RoutingContext) = withVertx(context.vertx()) {
    val body: DenizenRecord = context.bodyAsJson.toRecord()

    val search: List<DenizenRecord> =
            dbFindAsync(DenizenRecord().apply { denizenId = body.denizenId!!; email = body.email!! })

    if(search.isEmpty()) throw Unauthorized("Incorrect email-login combination")

    run<Unit> { sendJsonableAsync(Address.User.Auth.Restore, search.first()) }
    context.jsonResponse().end(JsonObject("succeeded" to true))
}

@HandlerFor(UrlPattern.Auth.ResetPassword)
@ForHttpMethod(HttpMethod.GET)
@Templatize("resetPassword.jade")
@JsBundle("resetPassword")
suspend fun resetPasswordPageHandler(context: RoutingContext) {
    use(context)
}

@HandlerFor(UrlPattern.Auth.RestorePassword)
@ForHttpMethod(HttpMethod.GET)
@Templatize("restorePassword.jade")
@JsBundle("restorePassword")
suspend fun restorePasswordPageHandler(context: RoutingContext) {
    val uid by context.request()
    try { UUID.fromString(uid) } catch(ex: Exception) { throw NotFound("No such page exists") }
    context.put("secret", uid)
}

@HandlerFor(UrlPattern.Auth.RestorePassword)
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
suspend fun restorePasswordEndpointHandler(context: RoutingContext) {
    val uid by context.request()
    val body: JsonObject = context.bodyAsJson
    body.retainFields("denizenId", "password")
    body["secret"] = uid

    run<Unit> { context.vertx().eventBus().sendJsonableAsync(Address.User.Auth.RestoreSecret, body) }
    context.jsonResponse().end(JsonObject("succeeded" to true))
}
