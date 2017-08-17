package org.jetbrains.research.kotoed.oauth

import io.vertx.core.Vertx

class Vk(vertx: Vertx, callbackBaseUri: String) : AbstractOAuthProvider(Name, vertx, callbackBaseUri) {
    override val providerBaseUri: String = "https://oauth.vk.com"

    suspend override fun doGetUserId(): String =
            getAccessTokenResponseBody().getInteger("user_id")?.toString() ?:
                    throw OAuthException("Cannot get VK user Id")

    companion object {
        val Name = "Vk"
    }
}
