package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import kotlinx.coroutines.experimental.launch

abstract class AsyncAuthProvider : AuthProvider, Loggable {
    protected abstract suspend fun doAuthenticateAsync(authInfo: JsonObject): User

    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        launch(UnconfinedWithExceptions(this)) coro@ {
            val user = try {
                doAuthenticateAsync(authInfo)
            } catch (ex: Exception) {
                handler.handle(Future.failedFuture(ex))
                return@coro
            }
            handler.handle(Future.succeededFuture(user))
        }
    }
}

suspend fun AuthProvider.authenticateAsync(authInfo: JsonObject) =
        vxa<User> { authenticate(authInfo, it)}

suspend fun <T: Jsonable> AuthProvider.authenticateJsonableAsync(authInfo: T) =
        authenticateAsync(authInfo.toJson())
