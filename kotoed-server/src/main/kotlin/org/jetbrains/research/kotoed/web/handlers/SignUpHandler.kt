package org.jetbrains.research.kotoed.web.handlers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.db.SignUpMsg
import org.jetbrains.research.kotoed.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.web.data.Auth

class SignUpHandler(private val authProvider: AuthProvider) : AsyncRoutingContextHandler() {
    override suspend fun doHandleAsync(context: RoutingContext) {
        val msg: SignUpMsg = try {
            fromJson(context.bodyAsJson)
        } catch (ex: IllegalArgumentException) {
            context.fail(HttpResponseStatus.BAD_REQUEST)
            return
        }

        val newDenizen: DenizenUnsafeRecord = try {
            context.vertx().eventBus().sendJsonableAsync(Address.User.Auth.SignUp, msg)
        } catch (ex: Exception) {
            context.response().end(Auth.SignUpResponse(false, ex.message ?: "Unknown remoteError"))
            return
        }

        use(newDenizen)

        // Sign up is done, now logging user in
        val session = context.session()
        val authInfo = JsonObject(
                "username" to msg.denizenId,
                "password" to msg.password
        )
        val user = try {
            vxa<User> { authProvider.authenticate(authInfo, it)}
        } catch (ex: Exception) {
            context.response().end(Auth.SignUpResponse(false, ex.message ?: "Unknown remoteError"))
            return
        } catch (nstt: NoStackTraceThrowable) {
            // We don't throw it in UavAuthProvider, but we're trying to be slightly more universal here
            context.response().end(Auth.SignUpResponse(false, nstt.message ?: "Unknown remoteError"))
            return
        }

        context.setUser(user)

        session?.regenerateId()
        context.response().end(Auth.SignUpResponse())
    }

}