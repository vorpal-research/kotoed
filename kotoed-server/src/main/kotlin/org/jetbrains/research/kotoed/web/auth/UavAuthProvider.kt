package org.jetbrains.research.kotoed.web.auth

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.data.db.InfoMsg
import org.jetbrains.research.kotoed.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

class UavAuthProvider(vertx: Vertx) : AsyncAuthProvider(vertx) {
    override suspend fun doAuthenticateAsync(authInfo: JsonObject): User {
        val patchedAI = authInfo.rename("username", "denizenId")

        val userLoginReply: JsonObject = try {
            vertx.eventBus().sendJsonableAsync(Address.User.Auth.Login, patchedAI)
        } catch (e: ReplyException) {
            if (e.failureCode() == HttpResponseStatus.FORBIDDEN.code()) {
                throw Unauthorized(e.message ?: "Unauthorized")
            } else {
                throw e
            }
        }
        val denizenId: String = userLoginReply["denizenId"] as String

        val info: DenizenUnsafeRecord = vertx
                .eventBus()
                .sendJsonableAsync(Address.User.Auth.Info, InfoMsg(denizenId = denizenId))

        val id = info.id

        return UavUser(vertx, denizenId, id)
    }
}