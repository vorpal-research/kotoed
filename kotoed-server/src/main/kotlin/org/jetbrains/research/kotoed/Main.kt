package org.jetbrains.research.kotoed

import io.netty.channel.DefaultChannelId
import io.vertx.core.*
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.RoutingConfig
import org.jetbrains.research.kotoed.util.routing.autoRegisterHandlers
import org.jetbrains.research.kotoed.util.template.helpers.KotoedUrlHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.auth.OAuthProvider
import org.jetbrains.research.kotoed.web.auth.UavAuthProvider
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    DefaultChannelId.newInstance() // warm-up slow DNS

    launch(Unconfined) { startApplication() }
}

val rootLog = LoggerFactory.getLogger(RootVerticle::class.java)

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

    val cf = vxa<CompositeFuture> { autoDeploy(vertx, it) }

    if (cf.failed()) {
        rootLog.error("Well funk me sideways...", cf.cause())
    }

    return vertx
}

@AutoDeployable
class RootVerticle : AbstractVerticle(), Loggable {

    override fun start(startFuture: Future<Void>) {
        val router = Router.router(vertx)

        log.info("Alive and standing")

        router.route().handler(BodyHandler.create())

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
