package org.jetbrains.research.kotoed.web.handlers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthHandler
import io.vertx.ext.web.handler.impl.AuthHandlerImpl
import org.jetbrains.research.kotoed.util.end

class RejectAnonymousHandler(authProvider: AuthProvider) : AuthHandlerImpl(authProvider) {
    override fun parseCredentials(context: RoutingContext, handler: Handler<AsyncResult<JsonObject>>) {
        // TODO: Should never be called
        handler.handle(Future.succeededFuture())
    }

    override fun handle(context: RoutingContext) {
        val session = context.session()
        if (session != null) {
            val user = context.user()
            if (user != null) {
                // Already logged in, just authorise
                authorize(user) { res ->
                    if (res.failed()) context.fail(res.cause())
                    else context.next()
                }
            } else {
                // Just reject this guy with 401
                context.response().end(HttpResponseStatus.UNAUTHORIZED)
            }
        } else {
            context.fail(NullPointerException("No session - did you forget to include a SessionHandler?"))
        }
    }

    companion object {
        fun create(authProvider: AuthProvider): AuthHandler = RejectAnonymousHandler(authProvider)
    }
}
