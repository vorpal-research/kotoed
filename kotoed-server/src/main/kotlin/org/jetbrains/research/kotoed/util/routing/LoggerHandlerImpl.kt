package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpVersion
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.impl.RouterImpl
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.web.UrlPattern
import java.util.*

class LoggerHandlerImpl: LoggerHandler, Loggable {
    protected fun doLog(status: Int, message: String?) {
        if (status >= 500) {
            log.error(message)
        } else if (status >= 400) {
            log.warn(message)
        } else {
            log.info(message)
        }
    }

    private fun log(
        context: RoutingContext,
        timestamp: Long,
        remoteClient: String,
        version: HttpVersion,
        method: HttpMethod,
        uri: String
    ) {
        val request = context.request()
        val contentLength = request.response().bytesWritten()
        val versionFormatted = when (version) {
            HttpVersion.HTTP_1_0 -> "HTTP/1.0"
            HttpVersion.HTTP_1_1 -> "HTTP/1.1"
            HttpVersion.HTTP_2 -> "HTTP/2.0"
        }
        val status = request.response().statusCode

        if (status == 200
            && (uri.startsWith("/eventbus/") || uri.startsWith("/static/"))
        ) return

        val message = String.format(
            "%s - %s %s %s %d %d",
            remoteClient,
            method,
            uri,
            versionFormatted,
            status,
            contentLength
        )
        doLog(status, message)
    }

    fun getClientAddress(request: HttpServerRequest): String {

        return request.getHeader("X-Real-IP")
            ?: request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.getHeader("Forwarded")?.split(";")?.firstNotNullOfOrNull {
                Regex("for=\\s*(.*)\\s*").matchEntire(it)?.groupValues?.getOrNull(1)
            }
            ?: request.remoteAddress().host()
    }

    override fun handle(context: RoutingContext) {
        val timestamp = System.currentTimeMillis()
        val remoteClient: String = getClientAddress(context.request())
        val method: HttpMethod = context.request().method()
        val uri: String = context.request().uri()
        val version: HttpVersion = context.request().version()

        context.addBodyEndHandler {
            log(context, timestamp, remoteClient, version, method, uri)
        }
        context.next()
    }
}