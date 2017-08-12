package org.jetbrains.research.kotoed.util

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

/*
 * Creating multiple handlers for the same Route is quite common use-case.
 * Unfortunately you have to recreate Route each time you add handler.
 * This class allows you to create a prototype for Route and build identical routes multiple times.
 */
class RouteProto(private val router: Router) {
    private val routeChunks = mutableListOf<RouteChunk>()

    companion object {
        private sealed class RouteChunk {
            abstract operator fun invoke(route: Route): Route

            data class Method(val method: HttpMethod) : RouteChunk() {
                override operator fun invoke(route: Route) = route.method(method)

            }

            data class Path(val path: String) : RouteChunk() {
                override operator fun invoke(route: Route) = route.path(path)
            }

            data class PathRegex(val path: String) : RouteChunk() {
                override operator fun invoke(route: Route) = route.pathRegex(path)
            }

            data class Produces(val contentType: String) : RouteChunk() {
                override operator fun invoke(route: Route) = route.produces(contentType)
            }

            data class Consumes(val contentType: String) : RouteChunk() {
                override operator fun invoke(route: Route) = route.produces(contentType)
            }

            data class Order(val order: Int) : RouteChunk() {
                override operator fun invoke(route: Route) = route.order(order)
            }

            object Last: RouteChunk() {
                override operator fun invoke(route: Route) = route.last()
            }

            data class Handler(val requestHandler: io.vertx.core.Handler<RoutingContext>): RouteChunk() {
                override operator fun invoke(route: Route) = route.handler(requestHandler)
            }

            data class BlockingHandler(val requestHandler: io.vertx.core.Handler<RoutingContext>,
                                       val ordered: Boolean = true): RouteChunk() {
                override operator fun invoke(route: Route) = route.blockingHandler(requestHandler, ordered)
            }

            data class FailureHandler(val requestHandler: io.vertx.core.Handler<RoutingContext>): RouteChunk() {
                override operator fun invoke(route: Route) = route.failureHandler(requestHandler)
            }

            object Remove: RouteChunk() {
                override operator fun invoke(route: Route) = route.remove()
            }

            object Disable: RouteChunk() {
                override operator fun invoke(route: Route) = route.disable()
            }

            object Enable: RouteChunk() {
                override operator fun invoke(route: Route) = route.enable()
            }

            data class UseNormalizedPath(val useNormalizedPath: Boolean): RouteChunk() {
                override operator fun invoke(route: Route) = route.useNormalisedPath(useNormalizedPath)
            }

        }
    }

    fun method(method: HttpMethod) = apply {
        routeChunks.add(RouteChunk.Method(method))
    }

    fun path(path: String) = apply {
        routeChunks.add(RouteChunk.Path(path))
    }

    fun pathRegex(path: String) = apply {
        routeChunks.add(RouteChunk.PathRegex(path))
    }

    fun produces(contentType: String) = apply {
        routeChunks.add(RouteChunk.Produces(contentType))
    }

    fun consumes(contentType: String) = apply {
        routeChunks.add(RouteChunk.Consumes(contentType))
    }

    fun order(order: Int) = apply {
        routeChunks.add(RouteChunk.Order(order))
    }

    fun last() = apply {
        routeChunks.add(RouteChunk.Last)
    }

    @Deprecated("forgot makeRoute()?", replaceWith = ReplaceWith(".makeRoute().handler()"))
    fun handler(requestHandler: io.vertx.core.Handler<RoutingContext>) = apply {
        routeChunks.add(RouteChunk.Handler(requestHandler))
    }

    @Deprecated("forgot makeRoute()?", replaceWith = ReplaceWith(".makeRoute().blockingHandler()"))
    fun blockingHandler(requestHandler: io.vertx.core.Handler<RoutingContext>, ordered: Boolean = true) = apply {
        routeChunks.add(RouteChunk.BlockingHandler(requestHandler, ordered))
    }

    @Deprecated("forgot makeRoute()?", replaceWith = ReplaceWith(".makeRoute().failureHandler()"))
    fun failureHandler(requestHandler: io.vertx.core.Handler<RoutingContext>, ordered: Boolean = true) = apply {
        use(ordered)
        routeChunks.add(RouteChunk.FailureHandler(requestHandler))
    }

    fun remove() = apply {
        routeChunks.add(RouteChunk.Remove)
    }

    fun disable() = apply {
        routeChunks.add(RouteChunk.Disable)
    }

    fun enable() = apply {
        routeChunks.add(RouteChunk.Enable)
    }

    fun useNormalizedPath(useNormalizedPath: Boolean) = apply {
        routeChunks.add(RouteChunk.UseNormalizedPath(useNormalizedPath))
    }

    fun makeRoute() = routeChunks.fold(router.route()) {route, chunk ->
        chunk(route)
    }

    fun branch(): RouteProto {
        val new = RouteProto(router)
        new.routeChunks.addAll(routeChunks)
        return new
    }

}

fun Router.routeProto() = RouteProto(this)
