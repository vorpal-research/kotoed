package org.jetbrains.research.kotoed.web.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

import org.jetbrains.research.kotoed.util.JsonObject

class UavUser(val vertx: Vertx,
              val denizedId: String,
              val id: Int) : User {

    override fun isAuthorised(authority: String, handler: Handler<AsyncResult<Boolean>>): User = apply {
        handler.handle(Future.succeededFuture(true)) // TODO how to check?
    }

    override fun clearCache(): User = this  // Cache? What cache?

    override fun setAuthProvider(ap: AuthProvider) {}  // Provider? Which provider?

    override fun principal(): JsonObject = JsonObject(
            "denizenId" to denizedId,
            "id" to id
    )
}