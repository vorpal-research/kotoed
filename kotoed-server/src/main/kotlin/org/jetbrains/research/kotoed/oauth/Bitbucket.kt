package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class Bitbucket(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri) {
    override val providerBaseUri: String = "https://bitbucket.org/site/oauth2/"
    private val apiUri = "https://api.bitbucket.org/2.0/"
    private val userEndpoint = "/user"

    override suspend fun doGetUserId(): String {
        val resp = webClient.getAbs("$apiUri/$userEndpoint".normalizeUri())
                .putHeader("${HttpHeaderNames.AUTHORIZATION}", "Bearer ${getAccessToken()}")
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()
        return resp.bodyAsJsonObject()
                ?.getString("account_id")
                ?: throw OAuthException("Cannot get Bitbucket id")
    }

    companion object {
        const val Name = "Bitbucket"
    }
}
