package org.jetbrains.research.kotoed.oauth

import io.vertx.core.Vertx

val KnownOAuthProviders =
        mapOf<String, (vx: Vertx) -> AbstractOAuthProvider>(
                Bitbucket.Name to { vx -> Bitbucket(vx) },
                Facebook.Name to { vx -> Facebook(vx) },
                GitHub.Name to { vx -> GitHub(vx) },
                Vk.Name to { vx -> Vk(vx) }
        )

fun getOAuthProvider(name: String, vertx: Vertx) =
        KnownOAuthProviders[name]?.invoke(vertx) ?: throw OAuthException("OAuth provider $name not found")