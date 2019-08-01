package org.jetbrains.research.kotoed.web.routers

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.eventbus.ReplyException
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
    name ?: return null

    return try {
        getOAuthProvider(name, context.vertx(), context.request().getRootUrl())
    } catch (ex: OAuthException) {
        return null
    }
}

@HandlerFor(UrlPattern.Auth.OAuthStart)
@EnableSessions
suspend fun handleOAuthStart(context: RoutingContext) {
    val providerName by context.request()

    val provider = providerOrNull(providerName, context) ?: run {
        context.fail(HttpResponseStatus.BAD_REQUEST)
        return
    }

    context.response().redirect(provider.getAuthorizeUriWithQuery())
}

@HandlerFor(UrlPattern.Auth.OAuthCallback)
@EnableSessions
class OAuthCallbackHandler(cfg: RoutingConfig) : AsyncRoutingContextHandler() {
    val authProvider = cfg.oAuthProvider

    suspend override fun doHandleAsync(context: RoutingContext) {
        val providerName by context.request()

        val provider = providerOrNull(providerName, context) ?: run {
            context.fail(HttpResponseStatus.BAD_REQUEST)
            return
        }

        val code by context.request()

        code ?: run {
            context.fail(HttpResponseStatus.BAD_REQUEST)
            return
        }

        provider.code = code!!

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
                // Making user introduce himself
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

            context.response().redirect(UrlPattern.Auth.LoginDone)
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
                    context.response().redirect(UrlPattern.Auth.LoginDone)
                    return
                } else {
                    context.fail(HttpResponseStatus.CONFLICT)
                    return
                }
            }

            val res: OauthProfileRecord = try {
                context.vertx().eventBus().sendJsonableAsync(Address.User.OAuth.SignUp,
                        OAuthSignUpMsg(
                                denizenId = context.user().principal().getString("denizenId"),
                                oauthProvider = provider.name,
                                oauthUser = oAuthUserId
                        )
                )
            } catch (ex: ReplyException) {
                // TODO Generally this can happen not only for login page.
                // TODO think about session error processor
                if (ex.failureCode() == HttpResponseStatus.CONFLICT.code()) {
                    context.session().destroy() // TODO this is fucked up
                    val url = UrlPattern.Auth.Index
                    val query = mapOf(
                            "conflict" to provider.name
                    ).makeUriQuery()
                    context.response().redirect(url + query)
                    return
                } else {
                    throw ex
                }
            }

            use(res)
            context.response().redirect(UrlPattern.Auth.LoginDone)
        }
    }

}
