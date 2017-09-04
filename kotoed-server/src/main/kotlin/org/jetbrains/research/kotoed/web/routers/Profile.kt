package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.getValue
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.routing.HandlerFor
import org.jetbrains.research.kotoed.util.routing.JsBundle
import org.jetbrains.research.kotoed.util.routing.LoginRequired
import org.jetbrains.research.kotoed.util.routing.Templatize
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority
import org.jetbrains.research.kotoed.web.navigation.NavBarContextName
import org.jetbrains.research.kotoed.web.navigation.kotoedNavBar

@HandlerFor(UrlPattern.Profile.Index)
@Templatize("profile.jade")
@LoginRequired
@JsBundle("profile")
suspend fun handleProfileIndex(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    if (!(context.user().isAuthorisedAsync(Authority.Teacher)
            || context.user().principal()?.getInteger("id") == intId)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(NavBarContextName, kotoedNavBar(context.user()))
}

@HandlerFor(UrlPattern.Profile.Edit)
@Templatize("profile.jade")
@LoginRequired
@JsBundle("profileEdit")
suspend fun handleProfileEdit(context: RoutingContext) {
    val id by context.request()
    val intId = id?.toIntOrNull()

    if (intId == null) {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    if (!(context.user().isAuthorisedAsync(Authority.Teacher)
            || context.user().principal()?.getInteger("id") == intId)) {
        context.fail(HttpResponseStatus.FORBIDDEN)
        return
    }

    context.put(NavBarContextName, kotoedNavBar(context.user()))
}
