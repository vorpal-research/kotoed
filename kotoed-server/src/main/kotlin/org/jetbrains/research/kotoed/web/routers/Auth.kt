package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.util.use
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.handlers.JsonLoginHandler
import org.jetbrains.research.kotoed.web.handlers.SignUpHandler

@HandlerFor(UrlPattern.Auth.Index)
@EnableSessions
@Templatize("login.jade")
@JsBundle("login")
@ChainedByHandler
fun loginPostHandlerFactory(context: RoutingContext) {
    if (context.user() != null) {
        context.response()
                .putHeader(HttpHeaders.LOCATION, UrlPattern.Auth.LoginDone)
                .end(HttpResponseStatus.FOUND)

    } else {
        context.next()
    }
}

@HandlerFactoryFor(UrlPattern.Auth.DoLogin)
@EnableSessions
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
fun doLoginHandlerFactory(cfg: RoutingConfig) = JsonLoginHandler(cfg.authProvider)

@HandlerFactoryFor(UrlPattern.Auth.DoSignUp)
@EnableSessions
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
fun doSignUpHandlerFactory(cfg: RoutingConfig) = SignUpHandler(cfg.authProvider)

@HandlerFor(UrlPattern.Auth.LoginDone)
@LoginRequired
fun loginDoneHandler(context: RoutingContext) {
    context.session()?.run {
        val returnUrl = remove<String>("return_url") ?: UrlPattern.Index
        context.response()
                .putHeader(HttpHeaders.LOCATION, returnUrl)
                .end(HttpResponseStatus.FOUND)
    }
}

@HandlerFactoryFor(UrlPattern.Auth.Logout)
@EnableSessions
fun logoutHandlerFactory(cfg: RoutingConfig) = run {
    use(cfg)
    LogoutHandler(UrlPattern.Index)
}

