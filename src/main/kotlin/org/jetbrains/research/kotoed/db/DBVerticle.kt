package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DebugRecord
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jetbrains.research.kotoed.util.ignore
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.jooq.UpdatableRecord

abstract class DatabaseVerticle<R: UpdatableRecord<R>>(
            val table: Table<R>,
            val entityName: String = table.name.toLowerCase()
        ): AbstractVerticle() {

    companion object {
        val DBPool =
            newFixedThreadPoolContext(Config.Debug.DB.PoolSize, "dbVerticles.dispatcher")
    }

    val dataSource get() = vertx.getSharedDataSource()
    val pk get() = table.primaryKey.fields.first() as Field<Any>

    val createAddress = Address.DB.create(entityName)
    val updateAddress = Address.DB.update(entityName)
    val readAddress   = Address.DB.read(entityName)
    val deleteAddress = Address.DB.delete(entityName)


    override fun start() {
        val eb = vertx.eventBus()
        eb.consumer<JsonObject>(createAddress){ handleCreate(it) }
        eb.consumer<JsonObject>(updateAddress){ handleUpdate(it) }
        eb.consumer<JsonObject>(readAddress){ handleRead(it) }
        eb.consumer<JsonObject>(deleteAddress){ handleDelete(it) }
    }

    private suspend fun<T> db(body: DSLContext.() -> T) =
        run(DBPool){ jooq(dataSource).use(body) }

    private fun DSLContext.selectById(id: Any) =
        select().from(table).where(pk.eq(id)).fetch().into(JsonObject::class.java).firstOrNull()

    open fun handleDelete(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        val resp = db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
        }
        message.reply(resp)
    }.ignore()

    open fun handleRead(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        val resp = db { selectById(id) }
        message.reply(resp)
    }.ignore()

    open fun handleUpdate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        val resp = db {
            update(table)
                    .set(newRecord(table, message.body().map))
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
        }
        message.reply(resp)
    }.ignore()

    open fun handleCreate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val resp = db {
            insertInto(table)
                    .set(newRecord(table, message.body().map))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
        }
        message.reply(resp)
    }.ignore()

}

class DebugVerticle: DatabaseVerticle<DebugRecord>(Tables.DEBUG)
class DenizenVerticle: DatabaseVerticle<DenizenRecord>(Tables.DENIZEN)
