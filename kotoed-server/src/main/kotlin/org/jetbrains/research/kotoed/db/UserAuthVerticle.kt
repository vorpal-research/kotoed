package org.jetbrains.research.kotoed.db

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jdbc.JDBCAuth
import io.vertx.ext.jdbc.JDBCClient
import org.jetbrains.research.kotoed.data.db.InfoMsg
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.data.db.SignUpMsg
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq

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

    @JsonableEventBusConsumerFor(Address.User.Auth.SignUp)
    suspend fun consumeSignUp(signUpMsg: SignUpMsg): JsonObject {
        val salt = authProvider.generateSalt()
        val password = authProvider.computeHash(signUpMsg.password, salt)

        val dbData = with(Tables.DENIZEN) {
            mapOf(
                    DENIZEN_ID.name to signUpMsg.denizenId,
                    PASSWORD.name to password,
                    SALT.name to salt
            )
        }

        return jooq(ds).use {
            with(Tables.DENIZEN) {
                it.insertInto(this)
                        .set(it.newRecord(this, dbData))
                        .returning(ID, DENIZEN_ID)
                        .fetchOne()
                        ?.into(JsonObject::class.java)
            }
        } ?: JsonObject()
    }

    @EventBusConsumerFor(Address.User.Auth.Login)
    suspend fun consumeLogin(msg: Message<JsonObject>) {
        fromJson<LoginMsg>(msg.body())

        val data = msg.body().rename("denizenId", "username")

        val user = vxa<User> { authProvider.authenticate(data, it) }

        msg.reply(user.principal().rename("username", "denizenId"))
    }

    @JsonableEventBusConsumerFor(Address.User.Auth.Info)
    suspend fun consumeInfo(infoMsg: InfoMsg): JsonObject {
        return jooq(ds).use {
            with(Tables.DENIZEN) {
                it.selectFrom(this)
                        .where(DENIZEN_ID.eq(infoMsg.denizenId))
                        .fetchKAsync()
                        .into(JsonObject::class.java)
                        .firstOrNull()
            }
        } ?: JsonObject()
    }
}
