package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.asMultiMap
import org.jetbrains.research.kotoed.util.makeUriQuery
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class Google(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri) {
    override val providerBaseUri: String = "https://accounts.google.com/"
    override val authorizeUri by lazy {
        "https://accounts.google.com/o/oauth2/v2/auth"
    }
    override val accessTokenUri: String by lazy {
        "https://www.googleapis.com/oauth2/v4/token"
    }

    override val scope by lazy {
        "https://www.googleapis.com/auth/userinfo.profile"
    }

    suspend override fun doGetUserId(): String {
        val query = mapOf(
                AccessToken to getAccessToken(),
                "alt" to "json"
        ).makeUriQuery()
        val resp = webClient.getAbs("https://www.googleapis.com/oauth2/v1/userinfo$query".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendAsync()
        return resp.bodyAsJsonObject()?.getString("id") ?: throw OAuthException("Cannot get Google id")
    }

    companion object {
        val Name = "Google"
    }
}
