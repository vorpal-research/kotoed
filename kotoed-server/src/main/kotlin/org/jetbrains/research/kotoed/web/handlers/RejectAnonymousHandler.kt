package org.jetbrains.research.kotoed.web.handlers

import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthHandler
import io.vertx.ext.web.handler.impl.AuthHandlerImpl
import org.apache.http.HttpStatus

interface RejectAnonymousHandler : AuthHandler {
    companion object {
        fun create(authProvider: AuthProvider): AuthHandler = RejectAnonymousHandlerImpl(authProvider)
    }
}

private class RejectAnonymousHandlerImpl(authProvider: AuthProvider) : AuthHandlerImpl(authProvider) {
    override fun handle(context: RoutingContext) {
        val session = context.session()
        if (session != null) {
            val user = context.user()
            if (user != null) {
                // Already logged in, just authorise
                authorise(user, context)
            } else {
                // Now redirect to the login url - we'll get redirected back here after successful login
                context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end()
            }
        } else {
            context.fail(NullPointerException("No session - did you forget to include a SessionHandler?"))
        }
    }
}