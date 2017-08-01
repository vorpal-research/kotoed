package org.jetbrains.research.kotoed.web.handlers


import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.end
import org.jetbrains.research.kotoed.util.fail

/**
 * Monkey-patched and Kotlinified FormLoginHandlerImpl that does not return 403 on auth failure
 */
class FormLoginHandlerWithRepeat(private val authProvider: AuthProvider,
                                 private val usernameParam: String = DEFAULT_USERNAME_PARAM,
                                 private val passwordParam: String = DEFAULT_PASSWORD_PARAM,
                                 private val returnURLParam: String = DEFAULT_RETURN_URL_PARAM,
                                 private val directLoggedInOKURL: String? = null) : Handler<RoutingContext>, Loggable {

    override fun handle(context: RoutingContext) {
        val req = context.request()
        if (req.method() != HttpMethod.POST) {
            context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED) // Must be a POST
        } else {
            if (!req.isExpectMultipart) {
                throw IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?")
            }
            val params = req.formAttributes()
            val username = params.get(usernameParam)
            val password = params.get(passwordParam)
            if (username == null || password == null) {
                log.warn("No username or password provided in form - did you forget to include a BodyHandler?")
                context.fail(HttpResponseStatus.BAD_REQUEST)
            } else {
                val session = context.session()
                val authInfo = JsonObject(
                        "username" to username,
                        "password" to password
                )
                authProvider.authenticate(authInfo, fun(res: AsyncResult<User>) {
                    if (res.succeeded()) {
                        context.setUser(res.result())
                        session?.run {
                            // the user has upgraded from unauthenticated to authenticated
                            // session should be upgraded as recommended by owasp
                            regenerateId()

                            val returnUrl = remove<String>(returnURLParam)

                            returnUrl?.run {
                                // Now redirect back to the original url
                                doRedirect(req.response(), returnUrl)
                                return
                            }
                        }

                        directLoggedInOKURL?.apply {
                            doRedirect(req.response(), this)
                        } ?: req.response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE)

                    } else {
                        context.put("loginError", res.cause()?.message)
                        context.next()
                    }
                })
            }
        }
    }

    private fun doRedirect(response: HttpServerResponse, url: String) {
        response.putHeader("location", url).end(HttpResponseStatus.FOUND)
    }

    companion object {
        val DEFAULT_USERNAME_PARAM = "username"
        val DEFAULT_PASSWORD_PARAM = "password"
        val DEFAULT_RETURN_URL_PARAM = "return_url"

        private val DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "<html><body><h1>Login successful</h1></body></html>"

        fun create(authProvider: AuthProvider,
                   usernameParam: String = DEFAULT_USERNAME_PARAM,
                   passwordParam: String = DEFAULT_PASSWORD_PARAM,
                   returnURLParam: String = DEFAULT_RETURN_URL_PARAM,
                   directLoggedInOKURL: String? = null) =
                FormLoginHandlerWithRepeat(
                        authProvider,
                        usernameParam,
                        passwordParam,
                        returnURLParam,
                        directLoggedInOKURL)
    }
}
