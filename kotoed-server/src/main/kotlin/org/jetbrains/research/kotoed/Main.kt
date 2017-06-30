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

        router.route()
                .failureHandler(this@RootVerticle::handleFailure)
        
        router.route("/static/*").handler(StaticHandler.create("webroot/static"))

        val authProvider = UavAuthProvider(vertx)

        router.autoRegisterHandlers(object: RoutingConfig {
            override val templateEngine: TemplateEngine =
                    JadeTemplateEngine.create()
            override val jsonFailureHandler: Handler<RoutingContext> =
                    Handler { routingContext -> handleFailure(routingContext) }
            override val htmlFailureHandler: Handler<RoutingContext> =
                    ErrorHandler.create()
            override val loggingHandler: Handler<RoutingContext> =
                    LoggerHandler.create(LoggerFormat.SHORT)
            override val staticFilesHelper: StaticFilesHelper =
                    StaticFilesHelper(vertx)
            override val templateHelpers: Map<String, TemplateHelper> =
                    mapOf("static" to staticFilesHelper)
            override val authProvider: AuthProvider = authProvider
            override val sessionStore: SessionStore = LocalSessionStore.create(vertx)
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
