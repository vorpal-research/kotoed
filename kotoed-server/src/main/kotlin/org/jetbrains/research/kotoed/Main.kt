package org.jetbrains.research.kotoed

import io.vertx.core.*
import io.vertx.core.json.JsonArray
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.ErrorHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.ext.web.templ.TemplateEngine
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.auth.UavAuthProvider
import org.jetbrains.research.kotoed.web.handlers.SessionProlongator

fun main(args: Array<String>) {
    launch(Unconfined) { startApplication() }
}

suspend fun startApplication(): Vertx {
    Thread.currentThread().contextClassLoader.getResourceAsStream(
            System.getProperty("kotoed.systemPropertiesFile", "system.properties")
    ).use {
        System.getProperties().load(it)
    }

    val vertx = clusteredVertxAsync(
            VertxOptions().also {
                it.metricsOptions = DropwizardMetricsOptions(
                        enabled = Config.Debug.Metrics.Enabled,
                        jmxEnabled = Config.Debug.Metrics.Enabled
                )
            }
    )

    //vertx.eventBus().addInterceptor(DebugInterceptor)

    vxa<CompositeFuture> { autoDeploy(vertx, it) }

    return vertx
}

@AutoDeployable
class RootVerticle : AbstractVerticle(), Loggable {

    override fun start(startFuture: Future<Void>) {
        val router = Router.router(vertx)

        log.info("Alive and standing")
        
        router.route("/static/*").handler(StaticHandler.create("webroot/static"))

        val authProvider = UavAuthProvider(vertx)

        router.autoRegisterHandlers(object: RoutingConfig {
            override val templateEngine =
                    JadeTemplateEngine.create()
            override val jsonFailureHandler =
                    Handler<RoutingContext> { routingContext -> handleFailure(routingContext) }
            override val htmlFailureHandler = ErrorHandler.create()
            override val loggingHandler = LoggerHandler.create(LoggerFormat.SHORT)
            override val staticFilesHelper = StaticFilesHelper(vertx)
            override val templateHelpers = mapOf("static" to staticFilesHelper)
            override val authProvider = authProvider
            override val sessionStore = LocalSessionStore.create(vertx)
            override val sessionProlongator = SessionProlongator.create()
        })

        vertx.createHttpServer()
                .requestHandler({ router.accept(it) })
                .listen(Config.Root.Port)

        startFuture.complete()
    }

    fun handleFailure(ctx: RoutingContext) {
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
}
