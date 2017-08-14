package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync

class Facebook(vertx: Vertx) : AbstractOAuthProvider(Name, vertx) {
    override val baseUri: String = "https://www.facebook.com/v2.10/"
    override val authorizeUri by lazy {
        "https://www.facebook.com/v2.10/dialog/oauth"
    }
    override val accessTokenUri: String by lazy {
        "https://graph.facebook.com/v2.10/oauth/access_token"
    }


    suspend override fun doGetUserId(): String {
        val resp = webClient.get("https://graph.facebook.com/v2.10/me".normalizeUri())
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_JSON}")
                .addQueryParam("fields", "id")
                .addQueryParam(AccessToken, getAccessToken())
                .sendAsync()
        return resp.bodyAsJsonObject()?.getInteger("id")?.toString() ?: throw OAuthException("Cannot get Facebook id")
    }

    companion object {
        val Name = "Facebook"
    }
}
