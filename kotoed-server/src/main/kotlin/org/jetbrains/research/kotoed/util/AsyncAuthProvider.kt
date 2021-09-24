package org.jetbrains.research.kotoed.util

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.research.kotoed.web.eventbus.guardian.cleanUp
import kotlin.coroutines.CoroutineContext

abstract class AsyncAuthProvider(vertx: Vertx) : AuthProvider, Loggable, CoroutineScope, WithVertx {
    val vertx = vertx
        @JvmName("vertx_") get;
    override fun getVertx(): Vertx = vertx

    override val coroutineContext: CoroutineContext by lazy { vertx.dispatcher() }

    protected abstract suspend fun doAuthenticateAsync(authInfo: JsonObject): User

    override fun authenticate(authInfo: JsonObject, handler: Handler<AsyncResult<User>>) {
        spawn(WithExceptions(handler)) coro@{
            handler.handle(Future.succeededFuture(doAuthenticateAsync(authInfo)))
        }
    }
}

suspend fun AuthProvider.authenticateAsync(authInfo: JsonObject) =
        vxa<User> { authenticate(authInfo, it) }

suspend fun <T : Jsonable> AuthProvider.authenticateJsonableAsync(authInfo: T) =
        authenticateAsync(authInfo.toJson())
