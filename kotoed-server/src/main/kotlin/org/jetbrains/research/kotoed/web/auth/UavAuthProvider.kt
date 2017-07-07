package org.jetbrains.research.kotoed.web.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.rename

class UavAuthProvider(private val vertx: Vertx) : AuthProvider {
    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        val patchedAI = authInfo.rename("username", "denizenId")
        vertx.eventBus().send<JsonObject>(Address.User.Auth.Login, patchedAI) {
            handler.handle(it.map { object : User { // TODO replace with proper user impl
                override fun isAuthorised(p0: String, p1: Handler<AsyncResult<Boolean>>): User = this

                override fun clearCache(): User = this

                override fun setAuthProvider(p0: AuthProvider?) {}

                override fun principal(): JsonObject = it.body()
            } })
        }
    }

}