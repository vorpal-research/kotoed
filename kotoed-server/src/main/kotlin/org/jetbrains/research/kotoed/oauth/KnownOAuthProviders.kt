package org.jetbrains.research.kotoed.oauth

import io.vertx.core.Vertx

val KnownOAuthProviders =
        mapOf<String, (vx: Vertx, callbackBaseUri: String) -> AbstractOAuthProvider>(
                Bitbucket.Name to ::Bitbucket,
                Facebook.Name to ::Facebook,
                GitHub.Name to ::GitHub,
                Vk.Name to ::Vk,
                Google.Name to ::Google,
                PolyCas.Name to ::PolyCas,
                PolyCasProd.Name to ::PolyCasProd
        )

fun getOAuthProvider(name: String, vertx: Vertx, callbackBaseUri: String) =
        KnownOAuthProviders[name]?.invoke(vertx, callbackBaseUri) ?:
                throw OAuthException("OAuth provider $name not found")