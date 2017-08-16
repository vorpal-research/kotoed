package org.jetbrains.research.kotoed

import io.vertx.core.*
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.util.template.helpers.KotoedUrlHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.auth.OAuthProvider
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
        
        router.initRoutes()

        vertx.createHttpServer()
                .requestHandler({ router.accept(it) })
                .listen(Config.Root.Port)

        startFuture.complete()
    }


    fun Router.initRoutes() {
        val staticFilesHelper = StaticFilesHelper(vertx)
        val routingConfig = RoutingConfig(
                vertx = vertx,
                templateEngine = JadeTemplateEngine.create().apply {
                    jadeConfiguration.isPrettyPrint = true
                },
                authProvider = UavAuthProvider(vertx),
                oAuthProvider = OAuthProvider(vertx),
                sessionStore = LocalSessionStore.create(vertx),
                templateHelpers = mapOf(
                        "static" to staticFilesHelper,
                        "url" to KotoedUrlHelper()
                ),
                staticFilesHelper = staticFilesHelper,
                loggingHandler = LoggerHandler.create(LoggerFormat.SHORT)
        )

        autoRegisterHandlers(routingConfig)
    }

}
