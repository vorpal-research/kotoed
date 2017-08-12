package org.jetbrains.research.kotoed

import io.vertx.core.*
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.util.template.helpers.StaticFilesHelper
import org.jetbrains.research.kotoed.web.auth.UavAuthProvider
import org.jetbrains.research.kotoed.web.eventbus.BridgeGuardian
import org.jetbrains.research.kotoed.web.eventbus.EventBusBridge
import org.jetbrains.research.kotoed.web.eventbus.guardian.KotoedFilter
import org.jetbrains.research.kotoed.web.eventbus.guardian.KotoedPatcher

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
        val authProvider = UavAuthProvider(vertx)
        val staticFilesHelper = StaticFilesHelper(vertx)
        val routingConfig = RoutingConfig(
                templateEngine = JadeTemplateEngine.create().apply {
                    this.jadeConfiguration.isPrettyPrint = true
                },
                authProvider = authProvider,
                sessionStore = LocalSessionStore.create(vertx),
                templateHelpers = mapOf("static" to staticFilesHelper),
                staticFilesHelper = staticFilesHelper,
                loggingHandler = LoggerHandler.create(LoggerFormat.SHORT),
                loginPath = "/doLogin"
        )

        route("/static/*").handler(StaticHandler.create("webroot/static"))

        routeProto().enableLogging(routingConfig)

        routeProto().path("/").enableSessions(routingConfig)

        createLoginRoute(routingConfig)
        createLogoutRoute(routingConfig)

        initEventBusBridge(routingConfig)

        autoRegisterHandlers(routingConfig)
    }

    fun Router.initEventBusBridge(routingConfig: RoutingConfig) {

        val filter = KotoedFilter(vertx)

        val bo = BridgeOptions().apply {
            for (po in filter.makePermittedOptions())
                addInboundPermitted(po)
        }

        val ebRouteProto = routeProto().path("/eventbus/*")

        ebRouteProto.requireLogin(routingConfig, rejectAnon = true)

        ebRouteProto.makeRoute().failureHandler(JsonFailureHandler)

        ebRouteProto.makeRoute().handler(EventBusBridge(vertx, bo, BridgeGuardian(filter, KotoedPatcher)))
    }
}
