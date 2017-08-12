package org.jetbrains.research.kotoed.util.routing

// Base route annotation. Router.route() or Router.routeWithRegex()
annotation class HandlerFor(val path: String, val isRegex: Boolean = false)

// Route annotations

// Route.method()
annotation class ForHttpMethod(val method: io.vertx.core.http.HttpMethod)
// Route.produces()
annotation class Produces(val contentType: String)
// Route.consumes()
annotation class Consumes(val contentType: String)
// Route.order()
annotation class Order(val order: Int)
// Route.last()
annotation class Last

// Handler annotations

// Add context.next() to the end of handler
annotation class Chain
// Add content-type: application/json to response
annotation class JsonResponse

// Apply TemplateHandler on this route after this handler
annotation class Templatize(val templateName: String, val withHelpers: Boolean = true)

annotation class JsBundle(val bundleName: String,
                          val withHash: Boolean = true,
                          val withJs: Boolean = true,
                          val withVendorJs: Boolean = true,
                          val withCss: Boolean = true,
                          val withVendorCss: Boolean = true,
                          val vendorName: String = "vendor")

annotation class LoginRequired
annotation class EnableSessions
