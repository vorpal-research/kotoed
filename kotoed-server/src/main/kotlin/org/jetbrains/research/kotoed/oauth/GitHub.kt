package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class GitHub(vertx: Vertx) : AbstractOAuthProvider(Name, vertx) {
    override val baseUri: String = "http://github.com/login/oauth/"
    private val apiUri = "https://api.github.com"
    private val userEndpoint = "/user"

    suspend override fun doGetUserId(): String {
        val resp = webClient.get("$apiUri/$userEndpoint".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .addQueryParam(AccessToken, getAccessToken())
                .sendAsync()
        return resp.bodyAsJsonObject()?.getInteger("id")?.toString() ?: throw OAuthException("Cannot get GitHub id")
    }

    companion object {
        val Name = "GitHub"
    }
}