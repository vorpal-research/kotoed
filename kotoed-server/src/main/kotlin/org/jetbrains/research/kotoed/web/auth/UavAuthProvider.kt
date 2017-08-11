package org.jetbrains.research.kotoed.web.auth

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.db.InfoMsg
import org.jetbrains.research.kotoed.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

class UavAuthProvider(private val vertx: Vertx) : AuthProvider, Loggable {

    private suspend fun authenticateAsync(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        val patchedAI = authInfo.rename("username", "denizenId")

        val userLoginReply: JsonObject

        try {
            userLoginReply = vertx.eventBus().sendJsonableAsync(Address.User.Auth.Login, patchedAI)
        } catch(e: ReplyException) {
            if (e.failureCode() == HttpResponseStatus.FORBIDDEN.code()) {
                handler.handle(Future.failedFuture(e.message))
                return
            } else
                throw e
        }
        try {
            val denizenId: String = userLoginReply["denizenId"] as String

            val info: DenizenUnsafeRecord = vertx
                    .eventBus()
                    .sendJsonableAsync(Address.User.Auth.Info, InfoMsg(denizenId = denizenId))

            val id = info.id

            handler.handle(Future.succeededFuture(UavUser(vertx, denizenId, id)))
        } catch (ex: Exception) {
            handler.handle(Future.failedFuture("Something went wrong"))
            throw ex
        }
    }

    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        launch(UnconfinedWithExceptions(this)) {
            authenticateAsync(authInfo, handler)
        }
    }
}