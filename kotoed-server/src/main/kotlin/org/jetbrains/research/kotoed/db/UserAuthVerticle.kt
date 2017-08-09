package org.jetbrains.research.kotoed.db

import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jdbc.JDBCAuth
import io.vertx.ext.jdbc.JDBCClient
import org.jetbrains.research.kotoed.data.db.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotoed.database.tables.records.OauthProfileRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jooq.conf.ParamType
import org.jooq.impl.DSL

@AutoDeployable
class UserAuthVerticle : DatabaseVerticle<DenizenUnsafeRecord>(Tables.DENIZEN_UNSAFE), Loggable {
    val theTable = Tables.DENIZEN_UNSAFE
    // FIXME akhin move to extension
    val authProvider: JDBCAuth
        get() = JDBCClient.create(vertx, dataSource)
                .let { JDBCAuth.create(vertx, it) }
                .apply {
                    val query = with(theTable) {
                        DSL.select(PASSWORD, SALT)
                                .from(theTable).where(DENIZEN_ID.equal(DSL.param(String::class.java)))
                                .getSQL(ParamType.INDEXED)
                    }
                    setAuthenticationQuery(query)
                }

    private fun DenizenUnsafeRecord.cleanup() = apply {
        password = null
        salt = null
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.SignUp)
    suspend fun consumeSignUp(signUpMsg: SignUpMsg): DenizenUnsafeRecord {
        val dbData = DenizenUnsafeRecord().apply {
            denizenId = signUpMsg.denizenId
            salt = authProvider.generateSalt()
            password = authProvider.computeHash(signUpMsg.password, salt)
        }

        return db {
            with(theTable) {
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
    suspend fun consumeInfo(infoMsg: InfoMsg): DenizenUnsafeRecord {
        return db {
            with(theTable) {
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
                                        theTable.ID,
                                        Tables.OAUTH_PROVIDER.ID,
                                        DSL.`val`(oauthSignUpMsg.oauthUser)
                                )
                                        .from(theTable, Tables.OAUTH_PROVIDER)
                                        .where(theTable.DENIZEN_ID.eq(oauthSignUpMsg.denizenId))
                                        .and(Tables.OAUTH_PROVIDER.NAME.eq(oauthSignUpMsg.oauthProvider))
                        )
                        .returning()
                        .fetchOne()
            }
        } ?: throw IllegalStateException("Database error")
    }

    @JsonableEventBusConsumerFor(Address.User.OAuth.Login)
    suspend fun consumeOAuthLogin(oauthLoginMsg: OAuthLoginMsg): DenizenUnsafeRecord {
        return db {
            select(*theTable.fields())
                    .from(Tables.OAUTH_PROFILE)
                    .join(Tables.OAUTH_PROVIDER)
                    .onKey()
                    .join(theTable)
                    .onKey()
                    .where(Tables.OAUTH_PROVIDER.NAME.eq(oauthLoginMsg.oauthProvider))
                    .and(Tables.OAUTH_PROFILE.OAUTH_USER_ID.eq(oauthLoginMsg.oauthUser))
                    .fetch()
                    .into(DenizenUnsafeRecord::class.java)
                    .firstOrNull()
                    ?.cleanup()
        } ?: throw NotFound("User '${oauthLoginMsg.oauthUser}' not found")
    }

    // TODO replace with something more appropriate (PermissionVerticle or smth)
    @JsonableEventBusConsumerFor(Address.User.Auth.HasPerm)
    suspend fun consumeHasPerm(msg: HasPermMsg): HasPermReply {
        val hasPerm = db {
            selectCount()
                    .from(theTable)
                    .join(Tables.DENIZEN_ROLE)
                    .on(Tables.DENIZEN_ROLE.DENIZEN_ID.eq(theTable.ID))
                    .join(Tables.ROLE_PERMISSION)
                    .on(Tables.ROLE_PERMISSION.ROLE_ID.eq(Tables.DENIZEN_ROLE.ROLE_ID))
                    .join(Tables.PERMISSION)
                    .on(Tables.ROLE_PERMISSION.PERMISSION_ID.eq(Tables.PERMISSION.ID))
                    .where(Tables.PERMISSION.NAME.eq(msg.perm))
                    .and(theTable.DENIZEN_ID.eq(msg.denizenId))
                    .firstOrNull()?.get(0) != 0
        }
        return HasPermReply(hasPerm)
    }
}
