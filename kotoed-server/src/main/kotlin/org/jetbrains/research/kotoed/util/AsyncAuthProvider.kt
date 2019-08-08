package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import kotlinx.coroutines.CoroutineName
import org.jetbrains.research.kotoed.web.eventbus.guardian.cleanUp

abstract class AsyncAuthProvider(val vertx: Vertx) : AuthProvider, Loggable {
    protected abstract suspend fun doAuthenticateAsync(authInfo: JsonObject): User

    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        val uuid = newRequestUUID()
        log.trace("Assigning $uuid to ${authInfo.cleanUp().toString().truncateAt(500)}")
        launchIn(WithExceptions(handler) + VertxContext(vertx) + CoroutineName(uuid)) coro@ {
            handler.handle(Future.succeededFuture(doAuthenticateAsync(authInfo)))
        }
    }
}

suspend fun AuthProvider.authenticateAsync(authInfo: JsonObject) =
        vxa<User> { authenticate(authInfo, it) }

suspend fun <T : Jsonable> AuthProvider.authenticateJsonableAsync(authInfo: T) =
        authenticateAsync(authInfo.toJson())
