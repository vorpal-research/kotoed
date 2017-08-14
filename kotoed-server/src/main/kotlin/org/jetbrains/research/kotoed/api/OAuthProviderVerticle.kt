package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import org.jetbrains.research.kotoed.database.tables.records.OauthProviderRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.oauth.KnownOAuthProviders
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor

@AutoDeployable
class OAuthProviderVerticle : AbstractKotoedVerticle()  {
    @JsonableEventBusConsumerFor(Address.Api.OAuthProvider.List)
    suspend fun handleOAuthProviderList(message: Unit): JsonArray {
        // We should know credentials
        val fromDb = dbFindAsync(OauthProviderRecord()).map { it.name }.toSet()

        // Also we should know how to do auth with this provider
        val fromMap = KnownOAuthProviders.keys

        return JsonArray(fromDb.intersect(fromMap).toList().sortedBy { it })
    }
}