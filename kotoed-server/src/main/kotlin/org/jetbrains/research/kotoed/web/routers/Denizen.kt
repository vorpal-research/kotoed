package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.navigation.Context
import org.jetbrains.research.kotoed.web.navigation.DenizenSearchBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.ProjectSearchBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.kotoedNavBar

@HandlerFor(UrlPattern.Denizen.Search)
@Templatize("denizenSearch.jade")
@LoginRequired
@JsBundle("denizenSearch")
@AuthorityRequired(Authority.Teacher)
suspend fun handleDenizenSearch(context: RoutingContext) {
    context.put(Context.NavBar, kotoedNavBar(context.user()))
    context.put(Context.BreadCrumb, DenizenSearchBreadCrumb)
}