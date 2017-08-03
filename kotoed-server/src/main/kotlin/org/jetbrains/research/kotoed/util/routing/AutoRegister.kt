package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.Handler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.*
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

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

private fun RouteProto.applyRouteAnnotation(annotation: Annotation) = when (annotation) {
    is ForHttpMethod -> method(annotation.method)
    is Produces -> produces(annotation.contentType)
    is Consumes -> consumes(annotation.contentType)
    is Order -> order(annotation.order)
    is Last -> last()
    else -> this
}

private fun RouteProto.applyRouteAnnotations(annotations: Array<Annotation>) =
        annotations.fold(this) { acc, anno -> acc.applyRouteAnnotation(anno) }

private fun Router.makeRouteProto(method: Method): RouteProto {
    val handlerForAnno = method.getAnnotation(HandlerFor::class.java)
    val annos = method.annotations

    val proto = routeProto()

    if (handlerForAnno.isRegex) {
        proto.pathRegex(handlerForAnno.path)
    } else {
        proto.path(handlerForAnno.path)
    }
    return proto.applyRouteAnnotations(annos)
}

private val CHAINING_ANNOTATIONS = listOf(
        Chain::class.java,
        Templatize::class.java
)

private fun Method.shouldChainHandler(): Boolean =
        CHAINING_ANNOTATIONS.any { getAnnotation(it) != null}

fun Router.autoRegisterHandlers(routingConfig: RoutingConfig) {
    val log = LoggerFactory.getLogger("org.jetbrains.research.kotoed.AutoRegisterHandlers")

    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
            .getMethodsAnnotatedWith(HandlerFor::class.java)
            .forEach { method ->
                log.trace("Auto-registering handler $method")

                val routeProto = makeRouteProto(method)

                method.getAnnotation(LoginRequired::class.java)?.apply {
                    routeProto.requireLogin(
                            routingConfig,
                            rejectAnon = method.getAnnotation(JsonResponse::class.java) != null)
                }

                method.getAnnotation(JsonResponse::class.java)?.apply {
                    routeProto.makeRoute().handler(PutJsonHeaderHandler)
                    routeProto.makeRoute().failureHandler(JsonFailureHandler)
                }

                routeProto.makeRoute()
                        .handler(funToHandler(method, method.shouldChainHandler()))


                method.getAnnotation(Templatize::class.java)?.apply {

                    if (this.withHelpers)
                        routeProto.enableHelpers(routingConfig)


                    method.getAnnotation(JsBundle::class.java)?.apply {

                        val vendorJs = if (withVendorJs) vendorName else null

                        val js = if (withJs) bundleName else null

                        val vendorCss = if (withVendorCss) vendorName else null

                        val css = if (withCss) bundleName else null

                        routeProto.enableJsBundle(
                                routingConfig,
                                JsBundleConfig(
                                        jsBundleName = js,
                                        vendorJsBundleName = vendorJs,
                                        cssBundleName = css,
                                        vendorCssBundleName = vendorCss
                                        ), withHash)
                    }

                    routeProto.templatize(routingConfig, templateName)
                    routeProto.makeRoute().failureHandler(HtmlFailureHandler)
                }
            }
}