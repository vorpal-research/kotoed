package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotoed.data.db.OAuthLoginMsg
import org.jetbrains.research.kotoed.data.db.OAuthSignUpMsg
import org.jetbrains.research.kotoed.database.tables.records.OauthProfileRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.oauth.AbstractOAuthProvider
import org.jetbrains.research.kotoed.oauth.OAuthException
import org.jetbrains.research.kotoed.oauth.getOAuthProvider
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.routing.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.handlers.JsonLoginHandler
import org.jetbrains.research.kotoed.web.handlers.SignUpHandler

@HandlerFor(UrlPattern.Auth.Index)
@EnableSessions
@Templatize("login.jade")
@JsBundle("login")
@ChainedByHandler
fun loginPostHandlerFactory(context: RoutingContext) {
    if (context.user() != null) {
        context.response().redirect(UrlPattern.Auth.LoginDone)
    } else {
        context.next()
    }
}

@HandlerFactoryFor(UrlPattern.Auth.DoLogin)
@EnableSessions
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
fun doLoginHandlerFactory(cfg: RoutingConfig) = JsonLoginHandler(cfg.authProvider)

@HandlerFactoryFor(UrlPattern.Auth.DoSignUp)
@EnableSessions
@ForHttpMethod(HttpMethod.POST)
@JsonResponse
fun doSignUpHandlerFactory(cfg: RoutingConfig) = SignUpHandler(cfg.authProvider)

@HandlerFor(UrlPattern.Auth.LoginDone)
@LoginRequired
fun loginDoneHandler(context: RoutingContext) {
    context.session()?.run {
        val returnUrl = remove<String>("return_url") ?: UrlPattern.Index
        context.response().redirect(returnUrl)
    }
}

@HandlerFactoryFor(UrlPattern.Auth.Logout)
@EnableSessions
fun logoutHandlerFactory(cfg: RoutingConfig) = run {
    use(cfg)
    LogoutHandler(UrlPattern.Index)
}

private fun providerOrNull(name: String?, context: RoutingContext): AbstractOAuthProvider? {
    name ?: run {
        return null
    }

    return try {
        getOAuthProvider(name, context.vertx())
    } catch (ex: OAuthException) {
        return null
    }
}

@HandlerFor(UrlPattern.Auth.OAuthStart)
@EnableSessions
fun handleOAuthStart(context: RoutingContext) {
    val providerName by context.request()

    val provider = providerOrNull(providerName, context) ?: run {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    context.response().redirect(provider.authorizeUri)
}

@HandlerFor(UrlPattern.Auth.OAuthCallback)
@EnableSessions
class OAuthCallbackHandler(cfg: RoutingConfig) : AsyncRoutingContextHandler() {
    val authProvider = cfg.oAuthProvider

    private fun RoutingContext.doRedirect() {
        val returnUrl = session()?.remove<String>("return_url") ?: UrlPattern.Index
        response().redirect(returnUrl)
    }

    suspend override fun doHandleAsync(context: RoutingContext) {
        val providerName by context.request()

        val provider = providerOrNull(providerName, context) ?: run {
            context.fail(HttpResponseStatus.BAD_REQUEST)
            return
        }

        val oAuthUserId = try {
            provider.getUserId()
        } catch (ex: OAuthException) {
            context.fail(HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (context.user() == null) {
            val user = try {
                authProvider.authenticateJsonableAsync(OAuthLoginMsg(provider.name, oAuthUserId))
            } catch (ex: KotoedException) {
                if (ex.code == HttpResponseStatus.UNAUTHORIZED.code()) {
                    val url = UrlPattern.Auth.Index
                    val query = mapOf(
                            "andThenOAuthWith" to provider.name
                    ).makeUriQuery()
                    context.response().redirect(url + query)
                    return
                } else {
                    throw ex
                }
            }

            context.setUser(user)

            context.session()?.regenerateId()

            context.doRedirect()
        } else {
            val user = try {
                authProvider.authenticateJsonableAsync(OAuthLoginMsg(provider.name, oAuthUserId))
            } catch (ex: KotoedException) {
                if (ex.code == HttpResponseStatus.UNAUTHORIZED.code()) {
                    null
                } else {
                    throw ex
                }
            }

            user?.run {
                if (principal() == context.user().principal()) {
                    context.doRedirect()
                    return
                } else {
                    context.fail(HttpResponseStatus.CONFLICT)
                    return
                }
            }

            val res: OauthProfileRecord = context.vertx().eventBus().sendJsonableAsync(Address.User.OAuth.SignUp,
                    OAuthSignUpMsg(
                            denizenId = context.user().principal().getString("denizenId"),
                            oauthProvider = provider.name,
                            oauthUser = oAuthUserId
                    )
            )
            use(res)
            context.doRedirect()
        }
    }

}