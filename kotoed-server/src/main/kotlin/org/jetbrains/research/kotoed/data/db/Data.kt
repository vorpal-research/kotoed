package org.jetbrains.research.kotoed.data.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.database.Public.PUBLIC
import org.jetbrains.research.kotoed.util.Jsonable
import org.jooq.Record
import org.jooq.TableRecord

/* UserAuthVerticle */

data class SignUpMsg(val denizenId: String, val password: String, val email: String?) : Jsonable
data class LoginMsg(val denizenId: String, val password: String) : Jsonable
data class InfoMsg(val denizenId: String) : Jsonable

data class OAuthSignUpMsg(
        val denizenId: String,
        val oauthProvider: String,
        val oauthUser: String) : Jsonable
data class OAuthLoginMsg(
        val oauthProvider: String,
        val oauthUser: String) : Jsonable
data class HasPermMsg(
        val denizenId: String,
        val perm: String
) : Jsonable
data class HasPermReply(
        val result: Boolean
) : Jsonable

fun newKotoedRecord(table: String) = PUBLIC.tables.find { it.name == table }?.newRecord()

data class BatchUpdateMsg<out R : Record>(val criteria: R, val patch: R) : Jsonable