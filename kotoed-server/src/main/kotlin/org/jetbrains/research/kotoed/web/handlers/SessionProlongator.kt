package org.jetbrains.research.kotoed.web.handlers

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.SessionHandler

class SessionProlongator(private var maxAge: Long = DEFAULT_MAX_AGE,
                         private var sessionCookieName: String = DEFAULT_SESSION_COOKIE_NAME): Handler<RoutingContext> {

    override fun handle(context: RoutingContext) {
        val cookie = context.getCookie(sessionCookieName)
        cookie.setMaxAge(maxAge)
        cookie.path = "/" // TODO maybe this should be a parameter but it's also hardcoded in SessionHandlerImpl
        context.next()
    }

    companion object {
        val DEFAULT_MAX_AGE = 30L * 24L * 60L * 60L
        val DEFAULT_SESSION_COOKIE_NAME = SessionHandler.DEFAULT_SESSION_COOKIE_NAME
        fun create(): SessionProlongator = SessionProlongator(DEFAULT_MAX_AGE, DEFAULT_SESSION_COOKIE_NAME)
    }
}
