package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.makeUriQuery
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class Polytech(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri), Loggable {
    override val providerBaseUri: String = "https://cas.icc.spbstu.ru/oauth2.0"
    override val accessTokenPath: String = "/token"

    override suspend fun doGetUserId(): String {
        val query = mapOf(
                AccessToken to getAccessToken()
        ).makeUriQuery()
        val resp = webClient.getAbs("$providerBaseUri/profile$query".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()

        return resp.bodyAsJsonObject()?.getString("objectSid") ?: throw OAuthException("Cannot get Polytech id")
    }


    companion object {
        val Name = "Polytech"
    }
}
