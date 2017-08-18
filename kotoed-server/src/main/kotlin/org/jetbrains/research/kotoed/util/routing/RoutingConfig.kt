package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.templ.TemplateEngine
import kotlinx.Warnings
import org.jetbrains.research.kotoed.util.RouteProto
import org.jetbrains.research.kotoed.util.template.NamedTemplateHandler
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.handlers.RejectAnonymousHandler
import org.jetbrains.research.kotoed.web.handlers.SessionProlongator

class RoutingConfig(
        val vertx: Vertx,
        val templateEngine: TemplateEngine,
        val authProvider: AuthProvider,
        val oAuthProvider: AuthProvider,
        sessionStore: SessionStore,
        templateHelpers: Map<String, TemplateHelper> = mapOf(),
        val staticFilesHelper: StaticFilesHelper,
        val loggingHandler: Handler<RoutingContext> = Handler {  },
        val loginPath: String = UrlPattern.Auth.Index,
        val staticLocalPath: String = "webroot/static"
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

    @Deprecated("You probably should call requireLogin()", ReplaceWith("requireLogin()"))
    fun requireLoginOnly(routeProto: RouteProto, rejectAnon: Boolean = false) {
        if (rejectAnon)
        // Basic auth handler might work here but we don't want to bypass login form with basic auth
            routeProto.makeRoute().handler(rejectAuthHandler)
        else
            routeProto.makeRoute().handler(redirectAuthHandler)
    }

    fun requireLogin(routeProto: RouteProto, rejectAnon: Boolean = false) {
        enableSessions(routeProto)
        @Suppress(Warnings.DEPRECATION)
        requireLoginOnly(routeProto, rejectAnon)
    }

    @Deprecated("You probably should call requireAuthority()", ReplaceWith("requireAuthority()"))
    fun requireAuthorityOnly(routeProto: RouteProto, authority: String) {
        routeProto.makeRoute().handler(RequireAuthorityHandler(authority))
    }

    fun requireAuthority(routeProto: RouteProto, authority: String, rejectAnon: Boolean = false) {
        requireLogin(routeProto, rejectAnon)
        @Suppress("DEPRECATION")
        requireAuthorityOnly(routeProto, authority)
    }


    fun addBodyHandler(routeProto: RouteProto) {
        routeProto.makeRoute().handler(BodyHandler.create())
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

@Deprecated("You probably should call requireLogin()", ReplaceWith("requireLogin()"))
fun RouteProto.requireLoginOnly(config: RoutingConfig, rejectAnon: Boolean = false) = apply {
    @Suppress("DEPRECATION")
    config.requireLoginOnly(this, rejectAnon)
}

fun RouteProto.requireLogin(config: RoutingConfig, rejectAnon: Boolean = false) = apply {
    config.requireLogin(this, rejectAnon)
}

@Deprecated("You probably should call requireAuthority()", ReplaceWith("requireAuthority()"))
fun RouteProto.requireAuthorityOnly(config: RoutingConfig, authority: String) = apply {
    @Suppress("DEPRECATION")
    config.requireAuthorityOnly(this, authority)
}

fun RouteProto.requireAuthority(config: RoutingConfig, authority: String, rejectAnon: Boolean = false) = apply {
    config.requireAuthority(this, authority, rejectAnon)
}


fun RouteProto.addBodyHandler(config: RoutingConfig) = apply {
    config.addBodyHandler(this)
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

