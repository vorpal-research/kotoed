package org.jetbrains.research.kotoed.db

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DebugRecord
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.database.fetchKAsync
import org.jetbrains.research.kotoed.util.database.getSharedDataSource
import org.jetbrains.research.kotoed.util.database.jooq
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.jooq.UpdatableRecord

abstract class DatabaseVerticle<R: UpdatableRecord<R>>(
            val table: Table<R>,
            val entityName: String = table.name.toLowerCase()
        ): AbstractVerticle() {
    val dataSource get() = vertx.getSharedDataSource()
    val pk get() = table.primaryKey.fields.first() as Field<Any>

    val createAddress = "kotoed.$entityName.create"
    val updateAddress = "kotoed.$entityName.update"
    val readAddress   = "kotoed.$entityName.read"
    val deleteAddress = "kotoed.$entityName.delete"


    override fun start() {
        val eb = vertx.eventBus()
        eb.consumer<JsonObject>(createAddress){ handleCreate(it) }
        eb.consumer<JsonObject>(updateAddress){ handleUpdate(it) }
        eb.consumer<JsonObject>(readAddress){ handleRead(it) }
        eb.consumer<JsonObject>(deleteAddress){ handleDelete(it) }
    }

    private suspend fun DSLContext.selectById(id: Any) =
        select().from(table).where(pk.eq(id)).fetchKAsync().into(JsonObject::class.java).firstOrNull()

    open fun handleDelete(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        jooq(dataSource).use {
            val resp = it
                    .delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
            message.reply(resp)
        }
    }

    open fun handleRead(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        jooq(dataSource).use {
            val resp = it.selectById(id)
            message.reply(resp)
        }
    }

    open fun handleUpdate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        val id = message.body().getValue(pk.name)
        jooq(dataSource).use {
            val resp = it
                    .update(table)
                    .set(it.newRecord(table, message.body().map))
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)

            message.reply(resp)
        }
    }

    open fun handleCreate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)){
        jooq(dataSource).use {
            val resp = it
                    .insertInto(table)
                    .set(it.newRecord(table, message.body().map))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)

            message.reply(resp)
        }
    }

}

class DebugVerticle: DatabaseVerticle<DebugRecord>(Tables.DEBUG)
class DenizenVerticle: DatabaseVerticle<DenizenRecord>(Tables.DENIZEN)
