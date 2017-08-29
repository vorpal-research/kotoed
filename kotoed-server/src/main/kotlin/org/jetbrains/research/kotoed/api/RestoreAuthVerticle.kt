package org.jetbrains.research.kotoed.api

import com.google.common.cache.CacheBuilder
import org.jetbrains.research.kotoed.data.api.RestorePasswordSecret
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.util.*
import java.util.concurrent.TimeUnit

@AutoDeployable
class RestoreAuthVerticle: AbstractKotoedVerticle(), Loggable {

    val requestCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build<String, UUID>()

    @JsonableEventBusConsumerFor(Address.User.Auth.Restore)
    suspend fun handleRestore(denizen: DenizenRecord) {
        log.info("Password restoration request from ${denizen.denizenId}")
        requestCache.put(denizen.denizenId, UUID.randomUUID())
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.RestoreSecret)
    suspend fun handleRestoreSecret(request: RestorePasswordSecret) {
        log.info("Password restoration callback from ${request.denizenId}" +
                "with secret '${request.secret}'")

        val secret = UUID.fromString(request.secret)
        if(request.denizenId in requestCache
                && requestCache[request.denizenId] == secret) {

            run<Unit> {
                sendJsonableAsync(Address.User.Auth.SetPassword, LoginMsg(request.denizenId, request.newPassword))
            }

        } else throw Forbidden("Illegal request")
    }

}