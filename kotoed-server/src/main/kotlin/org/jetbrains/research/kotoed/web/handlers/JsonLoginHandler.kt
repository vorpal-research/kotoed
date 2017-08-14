package org.jetbrains.research.kotoed.web.handlers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.web.data.Auth

class JsonLoginHandler(
        private val authProvider: AuthProvider) : AsyncHandler<RoutingContext>() {


    override suspend fun doHandleAsync(event: RoutingContext) {
        val context = event
        val req = context.request()
        if (req.method() != HttpMethod.POST) {
            context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED) // Must be a POST
        } else {
            // TODO is there a better way to deal with null values
            val msg: LoginMsg = try {
                fromJson(req.bodyAsync().toJsonObject())
            } catch (ex: IllegalArgumentException) {
                context.fail(HttpResponseStatus.BAD_REQUEST)
                return
            }
            val session = context.session()
            val authInfo = JsonObject(
                    "username" to msg.denizenId,
                    "password" to msg.password
            )
            val user = try {
                vxa<User> { authProvider.authenticate(authInfo, it)}
            } catch (ex: Exception) {
                context.response().end(Auth.LoginResponse(false, ex.message ?: "Unknown error"))
                return
            } catch (nstt: NoStackTraceThrowable) {
                // We don't throw it in UavAuthProvider, but we're trying to be slightly more universal here
                context.response().end(Auth.LoginResponse(false, nstt.message ?: "Unknown error"))
                return
            }

            context.setUser(user)

            session?.regenerateId()

            context.response().end(Auth.LoginResponse())

        }
    }


    companion object {
        fun create(authProvider: AuthProvider) =
                JsonLoginHandler(authProvider)
    }
}
