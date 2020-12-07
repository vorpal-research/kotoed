package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.asMultiMap
import org.jetbrains.research.kotoed.util.sendFormAsync

class Vk(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri) {
    override val providerBaseUri: String = "https://oauth.vk.com"

    override suspend fun doGetUserId(): String =
            getAccessTokenResponseBody().getInteger("user_id")?.toString() ?:
                    throw OAuthException("Cannot get VK user Id")

    override suspend fun getAccessTokenResponseBody(): JsonObject = accessTokenResponseBody ?: run {
        val formData = mapOf(
                ClientId to getClientId(),
                ClientSecret to getClientSecret(),
                Code to code,
                RedirectUri to redirectUri,
                GrantType to AuthorizationCode
        ).asMultiMap()

        val resp = webClient.postAbs(accessTokenUri)
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED}")
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendFormAsync(formData)
        return resp.bodyAsJsonObject()?.also { accessTokenResponseBody = it } ?: throw OAuthException("Empty access token response")
    }

    companion object {
        val Name = "Vk"
    }
}
