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
import org.jetbrains.research.kotoed.web.handlers.FormLoginHandlerWithRepeat
import org.jetbrains.research.kotoed.web.handlers.JsonLoginHandler
import org.jetbrains.research.kotoed.web.handlers.RejectAnonymousHandler
import org.jetbrains.research.kotoed.web.handlers.SessionProlongator

class RoutingConfig(
        val templateEngine: TemplateEngine,
        val authProvider: AuthProvider,
        sessionStore: SessionStore,
        templateHelpers: Map<String, TemplateHelper> = mapOf(),
        val staticFilesHelper: StaticFilesHelper,
        val loggingHandler: Handler<RoutingContext> = Handler {  },
        val loginPath: String = "/login",
        val mainPath: String = "/",
        val logoutPath: String = "/logout"
        ) {


    private val cookieHandler = CookieHandler.create()
    private val sessionHandler = SessionHandler.create(sessionStore)
    private val userSessionHandler = UserSessionHandler.create(authProvider)
    private val redirectAuthHandler = RedirectAuthHandler.create(authProvider, loginPath)
    private val rejectAuthHandler = RejectAnonymousHandler.create(authProvider)
    private val sessionProlongator = SessionProlongator.create()
    private val putHelpersHandler  = PutHelpersHandler.create(templateHelpers)

    fun enableSessions(routeProto: RouteProto) {
        routeProto.makeRoute().handler(cookieHandler)
        routeProto.makeRoute().handler(sessionHandler)
        routeProto.makeRoute().handler(userSessionHandler)
        routeProto.makeRoute().handler(sessionProlongator)
    }

    fun requireLogin(routeProto: RouteProto, rejectAnon: Boolean = false) {
        enableSessions(routeProto)
        if (rejectAnon)
            // Basic auth handler might work here but we don't want to bypass login form with basic auth
            routeProto.makeRoute().handler(rejectAuthHandler)
        else
            routeProto.makeRoute().handler(redirectAuthHandler)
    }

    fun enableLogging(routeProto: RouteProto) {
        routeProto.makeRoute().handler(loggingHandler)
    }

    fun createLoginRoute(router: Router) {
        val routeProto = router.routeProto().path(loginPath + "/doIt")
        val routeProtoWithPost = routeProto.branch().method(HttpMethod.POST)

        routeProto.makeRoute().handler(cookieHandler)
        routeProto.makeRoute().handler(sessionHandler)
        routeProto.makeRoute().handler(userSessionHandler)
        routeProto.makeRoute().handler(sessionProlongator)
        jsonify(routeProto)
        routeProtoWithPost.makeRoute().handler(
                JsonLoginHandler.create(authProvider))
    }

    fun createLogoutRoute(router: Router) {
        val routeProto = router.routeProto().path(logoutPath)
        enableSessions(routeProto)
        routeProto.makeRoute().handler(LogoutHandler(mainPath))
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

    fun jsonify(routeProto: RouteProto) {
        routeProto.makeRoute().handler(PutJsonHeaderHandler)
        routeProto.makeRoute().failureHandler(JsonFailureHandler)
    }
}

fun RouteProto.enableSessions(config: RoutingConfig) = apply {
    config.enableSessions(this)
}

fun RouteProto.requireLogin(config: RoutingConfig, rejectAnon: Boolean = false) = apply {
    config.requireLogin(this, rejectAnon)
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

fun RouteProto.jsonify() = apply {
    makeRoute().handler(PutJsonHeaderHandler)
    makeRoute().failureHandler(JsonFailureHandler)
}

