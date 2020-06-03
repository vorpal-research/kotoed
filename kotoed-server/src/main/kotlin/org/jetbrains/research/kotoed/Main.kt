package org.jetbrains.research.kotoed

import io.netty.channel.DefaultChannelId
import io.vertx.core.*
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.templ.jade.JadeTemplateEngine
import io.vertx.kotlin.ext.dropwizard.dropwizardMetricsOptionsOf
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.flywaydb.core.Flyway
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Public
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.routing.AsyncSessionStore
import org.jetbrains.research.kotoed.util.routing.RoutingConfig
import org.jetbrains.research.kotoed.util.routing.autoRegisterHandlers
import org.jetbrains.research.kotoed.util.template.helpers.KotoedUrlHelper
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.auth.OAuthProvider
import org.jetbrains.research.kotoed.web.auth.UavAuthProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    DefaultChannelId.newInstance() // warm-up slow DNS

    CoroutineScope(Dispatchers.Unconfined)
            .launch(CoroutineName("main function")) { startApplication() }
}

val rootLog: Logger = LoggerFactory.getLogger(RootVerticle::class.java)

suspend fun startApplication(): Vertx {
    Thread.currentThread().contextClassLoader.getResourceAsStream(
            System.getProperty("kotoed.systemPropertiesFile", "system.properties")
    ).use {
        System.getProperties().load(it)
    }

    val vertx = localVertx(
            VertxOptions().also {
                it.metricsOptions = dropwizardMetricsOptionsOf(
                        enabled = Config.Debug.Metrics.Enabled,
                        jmxEnabled = Config.Debug.Metrics.Enabled
                )
            }
    )

    rootLog.info("Warming up DB connection")
    try { vertx.getSharedDataSource().connection.close() } catch (ex: Exception) {
        rootLog.error("", ex)
    }

    rootLog.info("Migrating the migrations")

    val flyway = Flyway.configure()
            .schemas(Public.PUBLIC.name)
            .dataSource(vertx.getSharedDataSource())
            .sqlMigrationPrefix("V")
            .configuration(System.getProperties())
            .load()

    flyway.migrate()

    rootLog.info("Migration successful")

    //vertx.eventBus().addInterceptor(DebugInterceptor)

    vertx.eventBus().registerCodec(NonCopyJsonObjectCodec)
    vertx.eventBus().registerCodec(NonCopyJsonArrayCodec)

    val cf = vxa<CompositeFuture> { autoDeploy(vertx, it) }

    if (cf.failed()) {
        rootLog.error("Well funk me sideways...", cf.cause())
    }

    return vertx
}

@AutoDeployable
class RootVerticle : AbstractVerticle(), Loggable {

    override fun start(startPromise: Promise<Void>) {
        val router = Router.router(vertx)

        log.info("Alive and standing")

        //router.route().handler(BodyHandler.create())

        router.initRoutes()

        vertx.createHttpServer(
                HttpServerOptions().apply {
                    isCompressionSupported = true
                    isDecompressionSupported = true
                }
        )
                .requestHandler(router)
                .listen(Config.Root.ListenPort)

        startPromise.complete()
    }


    fun Router.initRoutes() {
        val staticFilesHelper = StaticFilesHelper()
        val routingConfig = RoutingConfig(
                vertx = vertx,
                templateEngine = JadeTemplateEngine.create(vertx).apply {
                    jadeConfiguration.isPrettyPrint = true
                },
                authProvider = UavAuthProvider(vertx),
                oAuthProvider = OAuthProvider(vertx),
                sessionStore =  AsyncSessionStore(vertx),
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
