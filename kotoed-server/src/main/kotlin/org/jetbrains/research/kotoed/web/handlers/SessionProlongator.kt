package org.jetbrains.research.kotoed.web.handlers

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.SessionHandler

interface SessionProlongator: Handler<RoutingContext> {
    fun setMaxAge(maxAge: Long)
    fun setSessionCookieName(name: String)

    companion object {
        val DEFAULT_MAX_AGE = 30L * 24L * 60L * 60L
        val DEFAULT_SESSION_COOKIE_NAME = SessionHandler.DEFAULT_SESSION_COOKIE_NAME
        fun create(): SessionProlongator = SessionProlongatorImpl(DEFAULT_MAX_AGE, DEFAULT_SESSION_COOKIE_NAME)
    }
}

internal class SessionProlongatorImpl(private var maxAge: Long,
                                      private var sessionCookieName: String) : SessionProlongator {
    override fun setMaxAge(maxAge: Long) {
        this.maxAge = maxAge
    }

    override fun setSessionCookieName(name: String) {
        this.sessionCookieName = sessionCookieName
    }

    override fun handle(context: RoutingContext) {
        val cookie = context.getCookie(sessionCookieName)
        cookie.setMaxAge(maxAge)
        context.next()
    }

}