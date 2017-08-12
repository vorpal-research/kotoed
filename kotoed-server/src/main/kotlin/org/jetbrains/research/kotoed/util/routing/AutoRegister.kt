package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.Handler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.*
import org.reflections.Reflections
import org.reflections.scanners.FieldAnnotationsScanner
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.slf4j.LoggerFactory
import java.lang.reflect.*

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

private class HandlerWithNext(private val underlying: Handler<RoutingContext>) : Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        underlying.handle(context)
        context.next()
    }

}

private fun Class<*>.getPublicConstructorOrNull(vararg klasses: Class<*>): Constructor<*>? {
    val constructor = try {
        getConstructor(*klasses)
    } catch (ex: NoSuchMethodException) {
        return null
    }

    if (!Modifier.isPublic(constructor.modifiers))
        return null

    return constructor
}


private fun instantiateHandler(routingConfig: RoutingConfig,
                               klass: Class<*>,
                               chain: Boolean = false): Handler<RoutingContext> {
    val objInst = klass.kotlin.objectInstance?.uncheckedCast<Handler<RoutingContext>>()

    if (objInst != null) {
        return if (chain)
            HandlerWithNext(objInst)
        else
            objInst
    }

    if (!Handler::class.java.isAssignableFrom(klass))
        throw IllegalArgumentException("Class $klass should implement Handler interface")
    val rcConstructor = klass.getPublicConstructorOrNull(RoutingConfig::class.java)
    val defaultConstructor = klass.getPublicConstructorOrNull()

    if (rcConstructor == null && defaultConstructor == null)
        throw IllegalArgumentException(
                "Class $klass should be constructible from RoutingConfig or default-constructible")

    val instance =
            (rcConstructor?.newInstance(routingConfig) ?: defaultConstructor!!.newInstance())
                    .uncheckedCast<Handler<RoutingContext>>()

    return if (chain)
        HandlerWithNext(instance)
    else
        instance
}

private fun invokeHandlerFactory(routingConfig: RoutingConfig,
                                 method: Method,
                                 chain: Boolean = false): Handler<RoutingContext> {
    val args = method.parameterCount

    if (args > 1)
        throw IllegalArgumentException("Handler factory should have zero or one parameter")

    val instance =
            (if (args == 1) method.invoke(null, routingConfig) else method.invoke(null))
                    .uncheckedCast<Handler<RoutingContext>>()

    return if (chain)
        HandlerWithNext(instance)
    else
        instance
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

private fun Router.makeRouteProto(annoEl: AnnotatedElement): RouteProto {
    val hf = annoEl.getAnnotation(HandlerFor::class.java)
    val hff = annoEl.getAnnotation(HandlerFactoryFor::class.java)
    val isRegex: Boolean
    val path: String

    if (hf != null) {
        isRegex = hf.isRegex
        path = hf.path
    } else if (hff != null) {
        isRegex = hff.isRegex
        path = hff.path
    } else {
        throw IllegalStateException("No base route annotation")
    }

    val annos = annoEl.annotations

    val proto = routeProto()

    if (isRegex) {
        proto.pathRegex(path)
    } else {
        proto.path(path)
    }
    return proto.applyRouteAnnotations(annos)
}

private val CHAINING_ANNOTATIONS = listOf(
        Chain::class.java,
        Templatize::class.java
)

private fun AnnotatedElement.shouldChainHandler(): Boolean =
        getAnnotation(ChainedByHandler::class.java) == null &&
                CHAINING_ANNOTATIONS.any { getAnnotation(it) != null}

fun Router.autoRegisterHandlers(routingConfig: RoutingConfig) {
    val log = LoggerFactory.getLogger("org.jetbrains.research.kotoed.AutoRegisterHandlers")

    fun <T : AnnotatedElement>processAnnotated(
            annoEl: T,
            createHandler: (routingConfig: RoutingConfig,
                            el: T,
                            chain: Boolean) -> Handler<RoutingContext>) {
        log.trace("Auto-registering handler $annoEl")

        val routeProto = makeRouteProto(annoEl)

        val logReq = annoEl.getAnnotation(LoginRequired::class.java)

        logReq?.apply {
            routeProto.requireLogin(
                    routingConfig,
                    rejectAnon = annoEl.getAnnotation(JsonResponse::class.java) != null)
        }


        annoEl.getAnnotation(EnableSessions::class.java)?.apply {
            if (logReq == null)  // LoginRequired implies EnableSessions
                routeProto.enableSessions(routingConfig)
            else
                log.warn("EnableSessions annotation is useless together with LoginRequired")
        }

        annoEl.getAnnotation(AddBodyHandler::class.java)?.apply {
            routeProto.addBodyHandler(routingConfig)
        }

        annoEl.getAnnotation(JsonResponse::class.java)?.apply {
            routeProto.jsonify()
        }

        routeProto.makeRoute()
                .handler(createHandler(routingConfig, annoEl, annoEl.shouldChainHandler()))


        annoEl.getAnnotation(Templatize::class.java)?.apply {

            if (this.withHelpers)
                routeProto.enableHelpers(routingConfig)


            annoEl.getAnnotation(JsBundle::class.java)?.apply {

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

    log.trace("Auto-registering function-based handlers")
    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
            .getMethodsAnnotatedWith(HandlerFor::class.java)
            .forEach { method ->
                processAnnotated(method) { _, el, chain ->
                    funToHandler(el, chain)
                }
            }

    log.trace("Auto-registering class-based handlers")
    Reflections("org.jetbrains.research.kotoed", TypeAnnotationsScanner(), SubTypesScanner())
            .getTypesAnnotatedWith(HandlerFor::class.java)
            .forEach { klass ->
                processAnnotated(klass) { rc, el, chain ->
                    instantiateHandler(rc, el, chain)
                }
            }


    log.trace("Auto-registering factory-based handlers")
    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
            .getMethodsAnnotatedWith(HandlerFactoryFor::class.java)
            .forEach { method ->
                processAnnotated(method) { rc, el, chain ->
                    invokeHandlerFactory(rc, el, chain)
                }
            }


}