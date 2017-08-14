package org.jetbrains.research.kotoed.web.auth

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.data.db.InfoMsg
import org.jetbrains.research.kotoed.data.db.OAuthLoginMsg
import org.jetbrains.research.kotoed.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

class OAuthProvider(private val vertx: Vertx) : AsyncAuthProvider() {
    override suspend fun doAuthenticateAsync(authInfo: JsonObject): User {
        val msg = fromJson<OAuthLoginMsg>(authInfo)

        val record: DenizenUnsafeRecord = try {
            vertx.eventBus().sendJsonableAsync(Address.User.OAuth.Login, msg)
        } catch(e: ReplyException) {
            if (e.failureCode() == HttpResponseStatus.NOT_FOUND.code()) {
                throw Unauthorized(e.message ?: "Unauthorized")
            } else {
                throw e
            }
        }

        return UavUser(vertx, record.denizenId, record.id)
    }
}