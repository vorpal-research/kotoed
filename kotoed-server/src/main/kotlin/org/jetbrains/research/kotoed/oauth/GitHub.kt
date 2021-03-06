package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class GitHub(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri) {
    override val providerBaseUri: String = "https://github.com/login/oauth/"
    private val apiUri = "https://api.github.com"
    private val userEndpoint = "/user"

    override suspend fun doGetUserId(): String {
        val resp = webClient.getAbs("$apiUri/$userEndpoint".normalizeUri())
                .putHeader("${HttpHeaderNames.AUTHORIZATION}", "token ${getAccessToken()}")
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()

        return resp.bodyAsJsonObject()?.getInteger("id")?.toString() ?: throw OAuthException("Cannot get GitHub id")
    }

    companion object {
        const val Name = "GitHub"
    }
}
