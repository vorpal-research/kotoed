package org.jetbrains.research.kotoed.util.routing

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.ErrorHandler
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.template.TemplateHelper

object PutJsonHeaderHandler : Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        context.jsonResponse()
        context.next()
    }

    fun create() = this
}

object JsonFailureHandler: Handler<RoutingContext>, Loggable {
    override fun handle(ctx: RoutingContext) {
        val ex = ctx.failure().unwrapped
        log.error("Exception caught while handling request to ${ctx.request().uri()}", ex)
        ctx.jsonResponse()
                .setStatus(ex)
                .end(
                        JsonObject(
                                "success" to false,
                                "error" to ex.message,
                                "code" to codeFor(ex),
                                "stacktrace" to JsonArray(
                                        ex.stackTrace
                                                .map { it.toString() }
                                                .toList()
                                )
                        )
                )
    }

    fun create() = this
}

class LogoutHandler(val redirectTo: String): Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        context.session().destroy()
        context.response().setStatus(HttpResponseStatus.FOUND).putHeader(HttpHeaders.LOCATION, redirectTo).end()
    }

    companion object {
        fun create(redirectTo: String) = LogoutHandler(redirectTo)
    }
}

class PutHelpersHandler(val helpers: Map<String, TemplateHelper>): Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        context.put("helpers", helpers)
        context.next()
    }

    companion object {
        fun create(helpers: Map<String, TemplateHelper>) = PutHelpersHandler(helpers)
    }
}

class PutJsBundle(val processedJsBundleConfig: JsBundleConfig): Handler<RoutingContext> {
    override fun handle(context: RoutingContext) {
        processedJsBundleConfig.jsBundleName?.run { context.put("js", this) }
        processedJsBundleConfig.cssBundleName?.run { context.put("css", this) }
        processedJsBundleConfig.vendorJsBundleName?.run { context.put("vendorJs", this) }
        processedJsBundleConfig.vendorCssBundleName?.run { context.put("vendorCss", this) }

        context.next()
    }

    companion object {
        fun create(processedJsBundleConfig: JsBundleConfig) = PutJsBundle(processedJsBundleConfig)
    }
}


object HtmlFailureHandler : Handler<RoutingContext> {
    private val handler = ErrorHandler.create()
    override fun handle(event: RoutingContext?) = handler.handle(event)  // Any other way to delegate it?

    fun create() = this
}