package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.makeUriQuery
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

/**
 * This is my impl of Apereo CAS client which I accidentally created while debugging it on my local machine.
 *
 * We can probably use it later if PolyCAS will be a thing.
 */
class PolyCas(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri), Loggable {
    override val providerBaseUri: String = "https://localhost:1488/oauth2.0"  // TODO definitely not localhost
    override val accessTokenPath: String = "/token"

    suspend override fun doGetUserId(): String {
        val query = mapOf(
                AccessToken to getAccessToken()
        ).makeUriQuery()
        val resp = webClient.getAbs("$providerBaseUri/profile$query".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()

        return resp.bodyAsJsonObject()?.getString("employeeID") ?: throw OAuthException("Cannot get PolyCas id")
    }


    companion object {
        val Name = "PolyCas"
    }
}
