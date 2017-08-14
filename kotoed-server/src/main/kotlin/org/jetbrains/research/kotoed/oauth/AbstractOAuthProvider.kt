package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.makeUriQuery
import org.jetbrains.research.kotoed.util.normalizeUri
import org.jetbrains.research.kotoed.util.sendAsync
import org.jetbrains.research.kotoed.web.UrlPattern
import java.net.URI
import java.net.URLEncoder

class OAuthException(message: String) : Exception(message)

abstract class AbstractOAuthProvider(val name: String, private val vertx: Vertx) {
    abstract val baseUri: String

    open val authorizePath: String = "/authorize"
    open val accessTokenPath: String = "/access_token"

    protected val webClient = WebClient.create(vertx)

    // Code is set externally
    var code: String? = null
        get() = field ?: throw IllegalStateException("Code is not obtained")

    private val codeUri: String? = null
    private val clientId: String? = null
    private val clientSecret: String? = null
    private val accessTokenResponseBody: JsonObject? = null
    private val accessToken: String? = null
    private val userId: String? = null

    suspend fun getClientId(): String = clientId ?: run {
        "SomeClientId"
    }

    suspend fun getClientSecret(): String = clientSecret ?: run {
        "SomeClientSecret"
    }

    open val redirectUri by lazy {
        "${Config.OAuth.BaseUrl}${UrlPattern.reverse(UrlPattern.Auth.OAuthCallback, mapOf("providerName" to name))}".normalizeUri()
    }

    open val authorizeUri by lazy {
        "$baseUri/$authorizePath".normalizeUri()
    }

    suspend fun getAuthorizeUriWithQuery() = codeUri ?: run {
        doGetAuthorizeUriWithQuery()
    }

    open protected suspend fun doGetAuthorizeUriWithQuery(): String {
        val query = mapOf(
                ClientId to getClientId(),
                RedirectUri to redirectUri
                ).makeUriQuery()
        return authorizeUri + query
    }

    open val accessTokenUri by lazy {
        "$baseUri/$accessTokenPath".normalizeUri()
    }

    suspend fun getAccessTokenResponseBody(): JsonObject = accessTokenResponseBody ?: run {
        val resp = webClient.postAbs(accessTokenUri)
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED}")
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .addQueryParam(ClientId, getClientId())
                .addQueryParam(ClientSecret, getClientSecret())
                .addQueryParam(Code, code)
                .addQueryParam(RedirectUri, redirectUri)
                .addQueryParam(GrantType, Code)
                .sendAsync()

        return resp.bodyAsJsonObject() ?: throw OAuthException("Empty access token response")
    }

    suspend fun getAccessToken(): String = accessToken ?: run {
        doGetAccessToken()
    }

    open protected suspend fun doGetAccessToken(): String {
        val json = getAccessTokenResponseBody()

        return json.getString(AccessToken) ?: throw OAuthException(json.getString("error") ?: "Unknown OAuth error")
    }

    suspend fun getUserId() = accessToken ?: run {
        doGetUserId()
    }

    abstract protected suspend fun doGetUserId(): String


    companion object {
        const val ClientId = "client_id"
        const val ClientSecret = "client_secret"
        const val RedirectUri = "redirect_uri"
        const val Code = "code"
        const val AccessToken = "access_token"
        const val GrantType = "grant_type"
    }
}