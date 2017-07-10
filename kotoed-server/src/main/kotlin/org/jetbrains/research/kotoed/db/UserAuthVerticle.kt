package org.jetbrains.research.kotoed.db

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jdbc.JDBCAuth
import io.vertx.ext.jdbc.JDBCClient
import org.jetbrains.research.kotoed.data.db.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.OauthProfileRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jooq.Record
import org.jooq.impl.DSL

@AutoDeployable
class UserAuthVerticle : AbstractKotoedVerticle(), Loggable {
    val ds get() = vertx.getSharedDataSource()

    // FIXME akhin move to extension
    val authProvider: JDBCAuth
        get() = JDBCClient.create(vertx, ds)
                .let { JDBCAuth.create(vertx, it) }
                .apply {
                    setAuthenticationQuery("SELECT password, salt FROM denizen WHERE denizen_id = ?")
                }

    private fun DenizenRecord.cleanup() = apply {
        password = null
        salt = null
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.SignUp)
    suspend fun consumeSignUp(signUpMsg: SignUpMsg): DenizenRecord {
        val dbData = DenizenRecord().apply {
            denizenId = signUpMsg.denizenId
            salt = authProvider.generateSalt()
            password = authProvider.computeHash(signUpMsg.password, salt)
        }

        return jooq(ds).use {
            with(Tables.DENIZEN) {
                it.insertInto(this)
                        .set(dbData)
                        .returning()
                        .fetchOne()
                        ?.cleanup()
            }
        } ?: throw IllegalStateException("Database error")
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.Login)
    suspend fun consumeLogin(msg: JsonObject): JsonObject {
        fromJson<LoginMsg>(msg)

        val data = msg.rename("denizenId", "username")

        val user = vxa<User> { authProvider.authenticate(data, it) }

        return user.principal().rename("username", "denizenId")
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.Info)
    suspend fun consumeInfo(infoMsg: InfoMsg): DenizenRecord {
        return jooq(ds).use {
            with(Tables.DENIZEN) {
                it.selectFrom(this)
                        .where(DENIZEN_ID.eq(infoMsg.denizenId))
                        .fetchKAsync()
                        .firstOrNull()
                        ?.cleanup()
            }
        } ?: throw NotFound("User '${infoMsg.denizenId}' not found")
    }

    @JsonableEventBusConsumerFor(Address.User.OAuth.SignUp)
    suspend fun consumeOAuthSignUp(oauthSignUpMsg: OAuthSignUpMsg): OauthProfileRecord {
        return jooq(ds).use {
            with(Tables.OAUTH_PROFILE) {
                it.insertInto(
                        this,
                        DENIZEN_ID,
                        OAUTH_PROVIDER_ID,
                        OAUTH_USER_ID
                )
                        .select(
                                it.select(
                                        Tables.DENIZEN.ID,
                                        Tables.OAUTH_PROVIDER.ID,
                                        DSL.`val`(oauthSignUpMsg.oauthUser)
                                )
                                        .from(Tables.DENIZEN, Tables.OAUTH_PROVIDER)
                                        .where(Tables.DENIZEN.DENIZEN_ID.eq(oauthSignUpMsg.denizenId))
                                        .and(Tables.OAUTH_PROVIDER.NAME.eq(oauthSignUpMsg.oauthProvider))
                        )
                        .returning()
                        .fetchOne()
            }
        } ?: throw IllegalStateException("Database error")
    }

    @JsonableEventBusConsumerFor(Address.User.OAuth.Login)
    suspend fun consumeOAuthLogin(oauthLoginMsg: OAuthLoginMsg): DenizenRecord {
        return jooq(ds).use {
            it.select(*Tables.DENIZEN.fields())
                    .from(Tables.OAUTH_PROFILE)
                    .join(Tables.OAUTH_PROVIDER)
                    .onKey()
                    .join(Tables.DENIZEN)
                    .onKey()
                    .where(Tables.OAUTH_PROVIDER.NAME.eq(oauthLoginMsg.oauthProvider))
                    .and(Tables.OAUTH_PROFILE.OAUTH_USER_ID.eq(oauthLoginMsg.oauthUser))
                    .fetchKAsync()
                    .into(DenizenRecord::class.java)
                    .firstOrNull()
                    ?.cleanup()
        } ?: throw NotFound("User '${oauthLoginMsg.oauthUser}' not found")
    }
}
