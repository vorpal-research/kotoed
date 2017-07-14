package org.jetbrains.research.kotoed.db

import io.vertx.core.eventbus.Message
import io.vertx.core.impl.NoStackTraceThrowable
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
class UserAuthVerticle : DatabaseVerticle<DenizenRecord>(Tables.DENIZEN), Loggable {
    // FIXME akhin move to extension
    val authProvider: JDBCAuth
        get() = JDBCClient.create(vertx, dataSource)
                .let { JDBCAuth.create(vertx, it) }
                .apply {
                    setAuthenticationQuery("SELECT password, salt FROM Denizen WHERE denizen_id = ?")
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

        return db {
            with(Tables.DENIZEN) {
                insertInto(this)
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

        val user =
            try {
                vxa<User> { authProvider.authenticate(data, it) }
            } catch (t: NoStackTraceThrowable) { // yes, vertx throws THIS b-t throwable here
                throw Forbidden(t.message ?: "Invalid username/password")
            }

        return user.principal().rename("username", "denizenId")
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.Info)
    suspend fun consumeInfo(infoMsg: InfoMsg): DenizenRecord {
        return db {
            with(Tables.DENIZEN) {
                selectFrom(this)
                        .where(DENIZEN_ID.eq(infoMsg.denizenId))
                        .fetch()
                        .firstOrNull()
                        ?.cleanup()
            }
        } ?: throw NotFound("User '${infoMsg.denizenId}' not found")
    }

    @JsonableEventBusConsumerFor(Address.User.OAuth.SignUp)
    suspend fun consumeOAuthSignUp(oauthSignUpMsg: OAuthSignUpMsg): OauthProfileRecord {
        return db {
            with(Tables.OAUTH_PROFILE) {
                insertInto(
                        this,
                        DENIZEN_ID,
                        OAUTH_PROVIDER_ID,
                        OAUTH_USER_ID
                )
                        .select(
                                select(
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
        return db {
            select(*Tables.DENIZEN.fields())
                    .from(Tables.OAUTH_PROFILE)
                    .join(Tables.OAUTH_PROVIDER)
                    .onKey()
                    .join(Tables.DENIZEN)
                    .onKey()
                    .where(Tables.OAUTH_PROVIDER.NAME.eq(oauthLoginMsg.oauthProvider))
                    .and(Tables.OAUTH_PROFILE.OAUTH_USER_ID.eq(oauthLoginMsg.oauthUser))
                    .fetch()
                    .into(DenizenRecord::class.java)
                    .firstOrNull()
                    ?.cleanup()
        } ?: throw NotFound("User '${oauthLoginMsg.oauthUser}' not found")
    }
}
