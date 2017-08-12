package org.jetbrains.research.kotoed.web.routers

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.util.use
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.handlers.FormLoginHandlerWithRepeat

@HandlerFactoryFor(UrlPattern.Auth.Index)
@EnableSessions
@AddBodyHandler
@ForHttpMethod(HttpMethod.POST)
@Templatize("login.jade")
@JsBundle("hello")
@ChainedByHandler
fun loginPostHandlerFactory(cfg: RoutingConfig) =
        FormLoginHandlerWithRepeat(
                cfg.authProvider,
                directLoggedInOKURL = UrlPattern.Index
                )

@HandlerFor(UrlPattern.Auth.Index)
@EnableSessions
@ForHttpMethod(HttpMethod.GET)
@Templatize("login.jade")
@JsBundle("hello")
fun loginGetHandler(context: RoutingContext) {
    use(context)
}


@HandlerFactoryFor(UrlPattern.Auth.Logout)
@EnableSessions
fun logoutHandlerFactory(cfg: RoutingConfig) = run {
    use(cfg)
    LogoutHandler(UrlPattern.Index)
}

