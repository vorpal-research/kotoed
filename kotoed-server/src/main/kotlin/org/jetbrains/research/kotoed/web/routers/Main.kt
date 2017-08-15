package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.fail
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.eventbus.courseByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.denizenByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.projectByIdOrNull
import org.jetbrains.research.kotoed.web.eventbus.submissionByIdOrNull
import org.jetbrains.research.kotoed.web.navigation.BreadCrumbContextName
import org.jetbrains.research.kotoed.web.navigation.NavBarContextName
import org.jetbrains.research.kotoed.web.navigation.SubmissionReviewBreadCrumb
import org.jetbrains.research.kotoed.web.navigation.kotoedNavBar

@HandlerFor(UrlPattern.Index)
@Templatize("main.jade")
@EnableSessions
@JsBundle("hello", withCss = false)
fun handleIndex(context: RoutingContext) {
    context.put("who", "Kotoed")
}



@HandlerFor(UrlPattern.Debug.Navigation)
@Templatize("main.jade")
@EnableSessions
@JsBundle("hello", withCss = false)
suspend fun handleDebugNav(context: RoutingContext) {
    val subId = context.request().getParam("submissionId").toIntOrNull() ?: run {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    val eb = context.vertx().eventBus()
    val submission = eb.submissionByIdOrNull(subId)!!
    val project = eb.projectByIdOrNull(submission.projectId)!!
    val author = eb.denizenByIdOrNull(project.denizenId)!!
    val course = eb.courseByIdOrNull(project.courseId)!!
    context.put(BreadCrumbContextName, SubmissionReviewBreadCrumb(course, author, project, submission))
    context.put(NavBarContextName, kotoedNavBar(context.user()))

}
