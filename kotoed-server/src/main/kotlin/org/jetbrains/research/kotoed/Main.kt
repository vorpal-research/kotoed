package org.jetbrains.research.kotoed

import io.vertx.core.*
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.JadeTemplateEngine
import io.vertx.kotlin.ext.dropwizard.DropwizardMetricsOptions
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import io.vertx.kotlin.ext.web.handler.sockjs.PermittedOptions
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.*
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
        
        router.route("/static/*").handler(StaticHandler.create("webroot/static"))

        val authProvider = UavAuthProvider(vertx)
        val staticFilesHelper = StaticFilesHelper(vertx)
        val routingShared = RoutingConfig(
                templateEngine = JadeTemplateEngine.create(),
                authProvider = authProvider,
                sessionStore = LocalSessionStore.create(vertx),
                templateHelpers = mapOf("static" to staticFilesHelper),
                staticFilesHelper = staticFilesHelper,
                loggingHandler = LoggerHandler.create(LoggerFormat.SHORT),
                loginTemplate = "login.jade",
                loginBundleConfig = JsBundleConfig(
                        jsBundleName = "hello"
                )
        )

        router.routeProto().enableLogging(routingShared)
        router.createLoginPageRoute(routingShared)
        router.createLogoutPageRoute(routingShared)

        router.autoRegisterHandlers(routingShared)

        val sockJSHandler = SockJSHandler.create(vertx)
        val po = PermittedOptions().setAddressRegex(".*")
        val options = BridgeOptions().addInboundPermitted(po)
        sockJSHandler.bridge(options) { be ->
            log.debug("------------------------")
            log.debug(be.type())
            log.debug(be.rawMessage)
            log.debug(be.socket().webUser())
            log.debug("------------------------")
            be.complete(true)
        }


        router.route("/eventbus/*").handler(sockJSHandler)

        // TODO

        vertx.createHttpServer()
                .requestHandler({ router.accept(it) })
                .listen(Config.Root.Port)

        startFuture.complete()
    }

}
