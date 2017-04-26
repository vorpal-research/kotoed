package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jdbc.JDBCAuth
import io.vertx.ext.jdbc.JDBCClient
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.db.InfoMsg
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.data.db.SignUpMsg
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq

class UserAuthVerticle : AbstractVerticle() {
    val ds get() = vertx.getSharedDataSource()

    // FIXME akhin move to extension
    val authProvider: JDBCAuth
        get() = JDBCClient.create(vertx, ds)
                .let { JDBCAuth.create(vertx, it) }
                .apply {
                    setAuthenticationQuery("SELECT password, salt FROM Denizen WHERE denizenId = ?")
                }

    override fun start() {
        registerAllConsumers()
    }

    @EventBusConsumerFor(Address.User.Auth.SignUp)
    fun consumeSignUp(msg: Message<JsonObject>): Unit = launch(UnconfinedWithExceptions(msg)) {
        val signUpMsg = fromJson<SignUpMsg>(msg.body())

        val salt = authProvider.generateSalt()
        val password = authProvider.computeHash(signUpMsg.password, salt)

        val dbData = mapOf(
                Tables.DENIZEN.DENIZENID.name to signUpMsg.denizenId,
                Tables.DENIZEN.PASSWORD.name to password,
                Tables.DENIZEN.SALT.name to salt
        )

        jooq(ds).use {
            val res = it.insertInto(Tables.DENIZEN)
                    .set(it.newRecord(Tables.DENIZEN, dbData))
                    .returning(Tables.DENIZEN.ID, Tables.DENIZEN.DENIZENID)
                    .fetchOne()
                    ?.into(JsonObject::class.java)

            msg.reply(res)
        }
    }.ignore()

    @EventBusConsumerFor(Address.User.Auth.Login)
    suspend fun consumeLogin(msg: Message<JsonObject>) {
        fromJson<LoginMsg>(msg.body())

        val data = msg.body().rename("denizenId", "username")

        val user = vxa<User> { authProvider.authenticate(data, it) }

        msg.reply(user.principal().rename("username", "denizenId"))
    }

    @EventBusConsumerFor(Address.User.Auth.Info)
    suspend fun consumeInfo(msg: Message<JsonObject>) {
        val infoMsg = fromJson<InfoMsg>(msg.body())

        jooq(ds).use {
            val res = it.selectFrom(Tables.DENIZEN)
                    .where(Tables.DENIZEN.DENIZENID.eq(infoMsg.denizenId))
                    .fetchKAsync()
                    .into(JsonObject::class.java)
                    .firstOrNull()

            msg.reply(res)
        }
    }
}
