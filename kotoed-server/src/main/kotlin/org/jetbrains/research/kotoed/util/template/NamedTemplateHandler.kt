package org.jetbrains.research.kotoed.util.template

import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.common.template.TemplateEngine
import org.jetbrains.research.kotoed.util.HttpHeaderValuesEx
import java.nio.file.Paths

class NamedTemplateHandler private constructor(
        val engine: TemplateEngine,
        val templateName: String,
        val templateDirectory: String = NamedTemplateHandler.DEFAULT_TEMPLATE_DIRECTORY,
        val contentType: String = NamedTemplateHandler.DEFAULT_CONTENT_TYPE) : Handler<RoutingContext> {


    override fun handle(context: RoutingContext) {
        val file = Paths.get(templateDirectory, templateName).toString()
        val data = context.data().toMutableMap()
        data["context"] = context
        engine.render(data, file) { res ->
            if (res.succeeded()) {
                context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType).end(res.result())
            } else {
                context.fail(res.cause())
            }
        }
    }

    companion object {
        val DEFAULT_TEMPLATE_DIRECTORY = "templates"
        val DEFAULT_CONTENT_TYPE = HttpHeaderValuesEx.HTML_UTF8

        fun create(engine: TemplateEngine,
                   templateName: String,
                   templateDirectory: String = DEFAULT_TEMPLATE_DIRECTORY,
                   contentType: String = DEFAULT_CONTENT_TYPE) =
                NamedTemplateHandler(engine, templateName, templateDirectory, contentType)
    }

}
