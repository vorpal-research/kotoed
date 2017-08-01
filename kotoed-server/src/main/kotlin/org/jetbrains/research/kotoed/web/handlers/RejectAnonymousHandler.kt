package org.jetbrains.research.kotoed.web.handlers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthHandler
import io.vertx.ext.web.handler.impl.AuthHandlerImpl
import org.jetbrains.research.kotoed.util.end

class RejectAnonymousHandler(authProvider: AuthProvider) : AuthHandlerImpl(authProvider) {
    override fun handle(context: RoutingContext) {
        val session = context.session()
        if (session != null) {
            val user = context.user()
            if (user != null) {
                // Already logged in, just authorise
                authorise(user, context)
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
