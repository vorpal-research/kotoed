package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.database.tables.records.OauthProviderRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.oauth.KnownOAuthProviders
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor

class OAuthProviderVerticle : AbstractKotoedVerticle()  {
    @JsonableEventBusConsumerFor(Address.Api.OAuthProvider.List)
    suspend fun handleOAuthProviderList(message: Unit): List<String> {
        // We should know credentials
        val fromDb = dbFindAsync(OauthProviderRecord()).map { it.name }.toSet()

        // Also we should know how to do auth with this provider
        val fromMap = KnownOAuthProviders.keys

        return fromDb.intersect(fromMap).toList().sortedBy { it }
    }
}