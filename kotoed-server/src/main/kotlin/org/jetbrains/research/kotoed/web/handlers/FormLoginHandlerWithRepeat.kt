package org.jetbrains.research.kotoed.web.handlers


import io.vertx.core.AsyncResult
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.Session
import io.vertx.ext.web.handler.FormLoginHandler
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.util.Loggable

/**
 * Monkey-patched FormLoginHandlerImpl that does not return 403 on auth failure
 */
class FormLoginHandlerWithRepeat(private val authProvider: AuthProvider,
                                 private var usernameParam: String,
                                 private var passwordParam: String,
                                 private var returnURLParam: String,
                                 private var directLoggedInOKURL: String?) : FormLoginHandler, Loggable {

    override fun setUsernameParam(usernameParam: String): FormLoginHandler = apply {
        this.usernameParam = usernameParam
    }

    override fun setPasswordParam(passwordParam: String): FormLoginHandler = apply {
        this.passwordParam = passwordParam
    }

    override fun setReturnURLParam(returnURLParam: String): FormLoginHandler = apply {
        this.returnURLParam = returnURLParam
    }

    override fun setDirectLoggedInOKURL(directLoggedInOKURL: String): FormLoginHandler = apply {
        this.directLoggedInOKURL = directLoggedInOKURL
    }

    override fun handle(context: RoutingContext) {
        val req = context.request()
        if (req.method() != HttpMethod.POST) {
            context.fail(405) // Must be a POST
        } else {
            if (!req.isExpectMultipart) {
                throw IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?")
            }
            val params = req.formAttributes()
            val username = params.get(usernameParam)
            val password = params.get(passwordParam)
            if (username == null || password == null) {
                log.warn("No username or password provided in form - did you forget to include a BodyHandler?")
                context.fail(400)
            } else {
                val session = context.session()
                val authInfo = JsonObject().put("username", username).put("password", password)
                authProvider.authenticate(authInfo, fun(res: AsyncResult<User>) {
                    if (res.succeeded()) {
                        val user = res.result()
                        context.setUser(user)
                        if (session != null) {
                            // the user has upgraded from unauthenticated to authenticated
                            // session should be upgraded as recommended by owasp
                            session.regenerateId()

                            val returnURL = session.remove<String>(returnURLParam)
                            if (returnURL != null) {
                                // Now redirect back to the original url
                                doRedirect(req.response(), returnURL)
                                return
                            }
                        }
                        directLoggedInOKURL?.apply {
                            doRedirect(req.response(), this)
                        } ?: req.response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE)
                    } else {
                        context.put("loginError", res.cause().message)
                        context.next()
                    }
                })
            }
        }
    }

    private fun doRedirect(response: HttpServerResponse, url: String) {
        response.putHeader("location", url).setStatusCode(302).end()
    }

    companion object {
        val DEFAULT_USERNAME_PARAM = "username"
        val DEFAULT_PASSWORD_PARAM = "password"
        val DEFAULT_RETURN_URL_PARAM = "return_url"

        private val DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "" + "<html><body><h1>Login successful</h1></body></html>"
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
