package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.templ.TemplateEngine
import org.jetbrains.research.kotoed.util.RouteProto
import org.jetbrains.research.kotoed.util.routeProto
import org.jetbrains.research.kotoed.util.template.NamedTemplateHandler
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.handlers.SessionProlongator

class RoutingConfig(
        private val templateEngine: TemplateEngine,
        private val authProvider: AuthProvider,
        sessionStore: SessionStore,
        templateHelpers: Map<String, TemplateHelper> = mapOf(),
        private val staticFilesHelper: StaticFilesHelper,
        private val loggingHandler: Handler<RoutingContext> = Handler {  },
        private val loginPath: String = "/login",
        private val mainPath: String = "/",
        private val logoutPath: String = "/logout",
        private val loginTemplate: String,
        private val loginBundleConfig: JsBundleConfig
        ) {


    private val cookieHandler = CookieHandler.create()
    private val sessionHandler = SessionHandler.create(sessionStore)
    private val userSessionHandler = UserSessionHandler.create(authProvider)
    private val authHandler = RedirectAuthHandler.create(authProvider, loginPath)
    private val sessionProlongator = SessionProlongator.create()
    private val putHelpersHandler  = PutHelpersHandler(templateHelpers)

    fun enableSessions(routeProto: RouteProto) {
        routeProto.makeRoute().handler(cookieHandler)
        routeProto.makeRoute().handler(sessionHandler)
        routeProto.makeRoute().handler(userSessionHandler)
//        routeProto.makeRoute().handler(sessionProlongator)
    }

    fun requireLogin(routeProto: RouteProto) {
        enableSessions(routeProto)
        routeProto.makeRoute().handler(authHandler)
    }

    fun enableLogging(routeProto: RouteProto) {
        enableSessions(routeProto)
        routeProto.makeRoute().handler(loggingHandler)
    }

    fun createLoginRoute(router: Router) {
        val routeProto = router.routeProto().path(loginPath)
        val routeProtoWithPost = routeProto.branch().method(HttpMethod.POST)

        routeProto.makeRoute().handler(cookieHandler)
        routeProto.makeRoute().handler(sessionHandler)
        routeProto.makeRoute().handler(userSessionHandler)
        routeProtoWithPost.makeRoute().handler(BodyHandler.create())
//        routeProto.makeRoute().handler(sessionProlongator)
        routeProtoWithPost.makeRoute().handler(
                FormLoginHandler.create(authProvider)
                        .setDirectLoggedInOKURL(mainPath))
        enableHelpers(routeProto)
        enableJsBundle(routeProto, loginBundleConfig, true)
        templatize(routeProto, loginTemplate)
    }

    fun createLogoutRoute(router: Router) {
        router.route(logoutPath).handler(LogoutHandler(mainPath))
    }

    fun enableHelpers(routeProto: RouteProto) {
        routeProto.makeRoute().handler(putHelpersHandler)
    }

    fun enableJsBundle(routeProto: RouteProto, jsBundleConfig: JsBundleConfig, withHash: Boolean = true) {
        val withHashes = JsBundleConfig(
                jsBundleName = jsBundleConfig.jsBundleName?.run {
                    staticFilesHelper.jsBundlePath(this, withHash)
                },
                cssBundleName = jsBundleConfig.cssBundleName?.run {
                    staticFilesHelper.cssBundlePath(this, withHash)
                },
                vendorJsBundleName = jsBundleConfig.vendorJsBundleName?.run {
                    staticFilesHelper.jsBundlePath(this, withHash)
                },
                vendorCssBundleName = jsBundleConfig.vendorCssBundleName?.run {
                    staticFilesHelper.cssBundlePath(this, withHash)
                }
        )

        routeProto.makeRoute().handler(PutJsBundle(withHashes))
    }

    fun templatize(routeProto: RouteProto, templateName: String) {
        routeProto.makeRoute().handler(NamedTemplateHandler.create(templateEngine, templateName))
    }

}

fun RouteProto.requireLogin(config: RoutingConfig) = apply {
    config.requireLogin(this)
}

fun RouteProto.enableLogging(config: RoutingConfig) = apply {
    config.enableLogging(this)
}

fun Router.createLoginRoute(config: RoutingConfig) = apply {
    config.createLoginRoute(this)
}

fun Router.createLogoutRoute(config: RoutingConfig) = apply {
    config.createLogoutRoute(this)
}

fun RouteProto.enableHelpers(config: RoutingConfig) = apply {
    config.enableHelpers(this)
}

fun RouteProto.enableJsBundle(config: RoutingConfig, jsBundleConfig: JsBundleConfig, withHash: Boolean = true) = apply {
    config.enableJsBundle(this, jsBundleConfig, withHash)
}

fun RouteProto.templatize(config: RoutingConfig, templateName: String) = apply {
    config.templatize(this, templateName)
}

