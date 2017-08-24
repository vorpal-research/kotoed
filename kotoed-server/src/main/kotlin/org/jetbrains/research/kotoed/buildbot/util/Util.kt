package org.jetbrains.research.kotoed.buildbot.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.ext.web.client.HttpRequest
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.research.kotoed.util.putHeader

fun <T> HttpRequest<T>.putDefaultBBHeaders() =
        this.putHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)

fun <T> HttpRequest<T>.isJson() =
        this.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)

object Kotoed2Buildbot {
    fun string2identifier(str: String): String {
        val cleaned = str.replace("\\W|^(?=\\d)".toRegex(), "_")
        val suffix = DigestUtils.sha512Hex(str).take(4)
        return "${cleaned}_$suffix"
    }

    fun courseName2endpoint(courseName: String) =
            DigestUtils.sha512Hex(courseName).take(32)

    fun projectName2builderName(projectName: String) =
            "buildbot_${string2identifier(projectName)}"

    fun projectName2schedulerName(projectName: String) =
            "scheduler_${string2identifier(projectName)}"
}
