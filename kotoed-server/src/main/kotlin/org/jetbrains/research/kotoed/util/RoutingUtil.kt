package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.templ.TemplateEngine
import org.jetbrains.research.kotoed.util.template.NamedTemplateHandler
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.handlers.SessionProlongator
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

// Base route annotation. Router.route() or Router.routeWithRegex()
annotation class HandlerFor(val path: String, val isRegex: Boolean = false)

// Route annotations

// Route.method()
annotation class ForHttpMethod(val method: io.vertx.core.http.HttpMethod)
// Route.produces()
annotation class Produces(val contentType: String)
// Route.consumes()
annotation class Consumes(val contentType: String)
// Route.order()
annotation class Order(val order: Int)
// Route.last()
annotation class Last

// Handler annotations

// Add context.next() to the end of handler
annotation class Chain
// Add content-type: application/json to response
annotation class JsonResponse

// Apply TemplateHandler on this route after this handler
annotation class Templatize(val templateName: String, val withHelpers: Boolean = true)

annotation class JsBundle(val bundleName: String,
                          val withHash: Boolean = true,
                          val withVendor: Boolean = true,
                          val withCss: Boolean = true,
                          val vendorName: String = "vendor")

annotation class LoginRequired

interface RoutingConfig {
    val templateEngine: TemplateEngine
    val templateHelpers: Map<String, TemplateHelper>
        get() = mapOf()
    val jsonFailureHandler: Handler<RoutingContext>
        get() = Handler {  }
    val htmlFailureHandler: Handler<RoutingContext>
        get() = Handler {  }
    val loggingHandler: Handler<RoutingContext>
        get() = Handler {  }
    val staticFilesHelper: StaticFilesHelper?
        get() = null
    val authProvider: AuthProvider
    val sessionStore: SessionStore
    val sessionProlongator: Handler<RoutingContext>
        get() = Handler {  }
    val loginPath: String
        get() = "/login"
    val mainPage: String
        get() = "/"
}

object PutJsonHeaderHandler : Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        context.jsonResponse()
        context.next()
    }
}

private inline fun <T> unwrapITE(body: () -> T) {
    try {
        body()
    } catch (ite: InvocationTargetException) {
        throw WrappedException(ite.cause)
    }
}

private fun funToHandler(method: Method, chain: Boolean = false): Handler<RoutingContext> {
    return when {
    // method.kotlinFunction.isSuspend does not work due to a bug in Kotlin =)
        method.isKotlinSuspend -> Handler {
            kotlinx.coroutines.experimental.launch(UnconfinedWithExceptions(it)) {
                unwrapITE {
                    method.invokeAsync(null, it)
                    if (chain)
                        it.next()
                }
            }
        }
        else -> Handler {
            DelegateLoggable(method.declaringClass).withExceptions(it) {
                unwrapITE {
                    method.invoke(null, it)
                    if (chain)
                        it.next()
                }
            }
        }
    }
}

private fun Route.applyRouteAnnotation(annotation: Annotation) = when (annotation) {
    is ForHttpMethod -> method(annotation.method)
    is Produces -> produces(annotation.contentType)
    is Consumes -> consumes(annotation.contentType)
    is Order -> order(annotation.order)
    is Last -> last()
    else -> this
}

private fun Route.applyRouteAnnotations(annotations: Array<Annotation>) =
        annotations.fold(this) { acc, anno -> acc.applyRouteAnnotation(anno) }

private fun Router.makeRoute(method: Method): Route {
    val handlerForAnno = method.getAnnotation(HandlerFor::class.java)
    val annos = method.annotations

    val route = if (handlerForAnno.isRegex) {
        routeWithRegex(handlerForAnno.path)
    } else {
        route(handlerForAnno.path)
    }
    return route.applyRouteAnnotations(annos)
}

private val CHAINING_ANNOTATIONS = listOf(
        Chain::class.java,
        Templatize::class.java
)

private fun Method.shouldChainHandler(): Boolean =
        CHAINING_ANNOTATIONS.any { getAnnotation(it) != null}

fun Router.autoRegisterHandlers(routingConfig: RoutingConfig) {
    val log = LoggerFactory.getLogger("org.jetbrains.research.kotoed.AutoRegisterHandlers")

    val cookieHandler = CookieHandler.create()
    val sessionHandler = SessionHandler.create(routingConfig.sessionStore)
    val userSessionHandler = UserSessionHandler.create(routingConfig.authProvider)
    val authHandler = RedirectAuthHandler.create(routingConfig.authProvider, routingConfig.loginPath)

    route().handler(routingConfig.loggingHandler)

    route(routingConfig.loginPath).handler(cookieHandler)
    route(routingConfig.loginPath).handler(sessionHandler)
    route(routingConfig.loginPath).handler(userSessionHandler)
    route(routingConfig.loginPath).method(HttpMethod.POST).handler(BodyHandler.create())
    route(routingConfig.loginPath).handler(routingConfig.sessionProlongator)
    route(routingConfig.loginPath).method(HttpMethod.POST).handler(
            FormLoginHandler.create(routingConfig.authProvider)
                    .setDirectLoggedInOKURL(routingConfig.mainPage))


    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
            .getMethodsAnnotatedWith(HandlerFor::class.java)
            .forEach { method ->
                log.trace("Auto-registering handler $method")

                method.getAnnotation(LoginRequired::class.java)?.apply {
                    makeRoute(method).handler(cookieHandler)
                    makeRoute(method).handler(sessionHandler)
                    makeRoute(method).handler(userSessionHandler)
                    makeRoute(method).handler(routingConfig.sessionProlongator)
                    makeRoute(method).handler(authHandler)
                }

                method.getAnnotation(JsonResponse::class.java)?.apply {
                    makeRoute(method).handler(PutJsonHeaderHandler)
                    makeRoute(method).failureHandler(routingConfig.jsonFailureHandler)
                }

                makeRoute(method)
                        .handler(funToHandler(method, method.shouldChainHandler()))


                method.getAnnotation(Templatize::class.java)?.apply {

                    if (this.withHelpers) {
                        makeRoute(method).handler {
                            it.put("helpers", routingConfig.templateHelpers)
                            it.next()
                        }

                    }

                    method.getAnnotation(JsBundle::class.java)?.apply {
                        val staticFilesHelper =
                                routingConfig.staticFilesHelper ?:
                                        throw IllegalStateException("In order to use JsBundle annotation " +
                                                "config.staticFilesHelper must be provided")

                        val vendorJs = if (withVendor)
                            staticFilesHelper.jsBundlePath(vendorName, withHash) else
                            null

                        val js = staticFilesHelper.jsBundlePath(bundleName, withHash)

                        val vendorCss = if (withCss)
                            staticFilesHelper.cssBundlePath(vendorName, withHash) else
                            null

                        val css = if (withVendor && withCss)
                            staticFilesHelper.cssBundlePath(bundleName, withHash) else
                            null

                        makeRoute(method).handler {
                            if (withVendor)
                                it.put("vendorJs", vendorJs)

                            it.put("js", js)

                            if (withVendor && withCss)
                                it.put("vendorCss", vendorCss)

                            if (withCss)
                                it.put("css", css)

                            it.next()
                        }
                    }

                    makeRoute(method).handler(
                            NamedTemplateHandler.create(routingConfig.templateEngine, templateName))
                    makeRoute(method).failureHandler(routingConfig.htmlFailureHandler)
                }
            }
}