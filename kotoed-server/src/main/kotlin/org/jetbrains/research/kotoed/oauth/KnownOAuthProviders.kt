package org.jetbrains.research.kotoed.oauth

import io.vertx.core.Vertx

val KnownOAuthProviders =
        mapOf<String, (vx: Vertx) -> AbstractOAuthProvider>(
                Bitbucket.Name to ::Bitbucket,
                Facebook.Name to ::Facebook,
                GitHub.Name to ::GitHub,
                Vk.Name to ::Vk,
                Google.Name to ::Google
        )

fun getOAuthProvider(name: String, vertx: Vertx) =
        KnownOAuthProviders[name]?.invoke(vertx) ?: throw OAuthException("OAuth provider $name not found")