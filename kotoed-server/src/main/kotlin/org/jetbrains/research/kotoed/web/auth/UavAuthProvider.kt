package org.jetbrains.research.kotoed.web.auth

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
import org.jetbrains.research.kotoed.util.database.toRecord

class UavAuthProvider(private val vertx: Vertx) : AuthProvider, Loggable {

    private suspend fun authenticateAsync(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        val patchedAI = authInfo.rename("username", "denizenId")

        val userLoginReply: JsonObject

        try {
            userLoginReply = vertx.eventBus().sendAsync(Address.User.Auth.Login, patchedAI).body()
        } catch(e: ReplyException) {
            handler.handle(Future.failedFuture(e.message))
            return
        }

        val denizenId: String? = userLoginReply?.get("denizenId") as? String

        if (denizenId == null) {
            handler.handle(Future.failedFuture("Something went wrong"))
            throw IllegalStateException("No denizenId in Address.User.Auth.Login reply")
        }

        val info = vertx
                .eventBus()
                .sendAsync(Address.User.Auth.Info, InfoMsg(denizenId = denizenId).toJson())
                .body()
                .toRecord<DenizenUnsafeRecord>()

        val id = info.id

        if (id == null) {
            handler.handle(Future.failedFuture("Something went wrong"))
            throw IllegalStateException("No id in Address.User.Auth.Info reply")
        }

        handler.handle(Future.succeededFuture(UavUser(vertx, denizenId, id)))
    }

    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        launch(UnconfinedWithExceptions(this)) {
            authenticateAsync(authInfo, handler)
        }
    }
}