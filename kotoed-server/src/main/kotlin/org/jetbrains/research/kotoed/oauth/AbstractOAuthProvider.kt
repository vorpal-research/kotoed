package org.jetbrains.research.kotoed.oauth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.OauthProviderRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.web.UrlPattern


class OAuthException(message: String) : Exception(message)

abstract class AbstractOAuthProvider(
        val name: String,
        private val vertx: Vertx,
        val callbackBaseUri: String) : Loggable {
    abstract val providerBaseUri: String

    open val authorizePath: String = "/authorize"
    open val accessTokenPath: String = "/access_token"
    open val scope: String = ""

    protected val webClient = WebClient.create(vertx)

    // Code is set externally
    private var _code: String? = null
    var code: String
        get() = _code ?: throw IllegalStateException("Code is not obtained")
        set(value: String) {
            _code = value
        }
    private var codeUri: String? = null

    private var accessTokenResponseBody: JsonObject? = null
    private var accessToken: String? = null
    private var userId: String? = null
    private var providerDbRecord: OauthProviderRecord? = null

    private suspend fun getProviderDbRecord() = providerDbRecord ?: run {
        val records: JsonArray = vertx.eventBus().
                sendJsonableAsync(Address.DB.find(
                        Tables.OAUTH_PROVIDER.name), OauthProviderRecord().apply {
                    name = this@AbstractOAuthProvider.name
                })

        val record = records[0]?.uncheckedCastOrNull<JsonObject>()
        return record?.toRecord<OauthProviderRecord>()?.also { providerDbRecord = it } ?:
                throw OAuthException("OAuth provider is not present in DB")
    }

    suspend fun getClientId(): String = getProviderDbRecord().clientId

    suspend fun getClientSecret(): String = getProviderDbRecord().clientSecret

    open val redirectUri by lazy {
        "$callbackBaseUri${UrlPattern.reverse(UrlPattern.Auth.OAuthCallback, mapOf("providerName" to name))}".normalizeUri()
    }

    open val authorizeUri by lazy {
        "$providerBaseUri/$authorizePath".normalizeUri()
    }

    suspend fun getAuthorizeUriWithQuery() = codeUri ?: run {
        doGetAuthorizeUriWithQuery().also { codeUri = it }
    }

    open protected suspend fun doGetAuthorizeUriWithQuery(): String {
        val query = mapOf(
                ClientId to getClientId(),
                RedirectUri to redirectUri,
                ResponseType to Code,
                Scope to scope
                ).makeUriQuery()
        return authorizeUri + query
    }

    open val accessTokenUri by lazy {
        "$providerBaseUri/$accessTokenPath".normalizeUri()
    }

    suspend fun getAccessTokenResponseBody(): JsonObject = accessTokenResponseBody ?: run {
        val formData = mapOf(
                ClientId to getClientId(),
                ClientSecret to getClientSecret(),
                Code to code,
                RedirectUri to redirectUri,
                GrantType to AuthorizationCode
        ).asMultiMap()

        val resp = webClient.postAbs(accessTokenUri)
                .putHeader("${HttpHeaderNames.CONTENT_TYPE}", "${HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED}")
                .putHeader("${HttpHeaderNames.ACCEPT}", "${HttpHeaderValues.APPLICATION_JSON}")
                .sendFormAsync(formData)
        return resp.bodyAsJsonObject()?.also { accessTokenResponseBody = it } ?: throw OAuthException("Empty access token response")
    }

    suspend fun getAccessToken(): String = accessToken ?: run {
        doGetAccessToken().also { accessToken = it }
    }

    open protected suspend fun doGetAccessToken(): String {
        val json = getAccessTokenResponseBody()

        return json.getString(AccessToken) ?: throw OAuthException(json.getString("error") ?: "Unknown OAuth error")
    }

    suspend fun getUserId() = userId ?: run {
        doGetUserId().also { userId = it }
    }

    abstract protected suspend fun doGetUserId(): String


    companion object {
        const val ClientId = "client_id"
        const val ClientSecret = "client_secret"
        const val RedirectUri = "redirect_uri"
        const val Code = "code"
        const val AccessToken = "access_token"
        const val GrantType = "grant_type"
        const val AuthorizationCode = "authorization_code"
        const val ResponseType = "response_type"
        const val Scope = "scope"
    }
}