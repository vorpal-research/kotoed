package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.makeUriQuery
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class GitHub(vertx: Vertx) : AbstractOAuthProvider(Name, vertx) {
    override val baseUri: String = "https://github.com/login/oauth/"
    private val apiUri = "https://api.github.com"
    private val userEndpoint = "/user"

    suspend override fun doGetUserId(): String {
        val query = mapOf(
                AccessToken to getAccessToken()
        ).makeUriQuery()
        val resp = webClient.getAbs("$apiUri/$userEndpoint$query".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()

        return resp.bodyAsJsonObject()?.getInteger("id")?.toString() ?: throw OAuthException("Cannot get GitHub id")
    }

    companion object {
        val Name = "GitHub"
    }
}