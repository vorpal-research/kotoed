package org.jetbrains.research.kotoed.web.handlers

import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthHandler
import io.vertx.ext.web.handler.impl.AuthHandlerImpl
import org.apache.http.HttpStatus

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
                context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end()
            }
        } else {
            context.fail(NullPointerException("No session - did you forget to include a SessionHandler?"))
        }
    }

    companion object {
        fun create(authProvider: AuthProvider): AuthHandler = RejectAnonymousHandler(authProvider)
    }
}
