package org.jetbrains.research.kotoed.db

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.teamcity.util.DimensionLocator
import org.jetbrains.research.kotoed.teamcity.util.TeamCityApi
import org.jetbrains.research.kotoed.teamcity.util.plus
import org.jetbrains.research.kotoed.teamcity.util.putDefaultTCHeaders
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.*

abstract class DatabaseVerticle<R : UpdatableRecord<R>>(
        val table: Table<R>,
        val entityName: String = table.name.toLowerCase()
) : AbstractVerticle(), Loggable {

    companion object {
        val DBPool =
                newFixedThreadPoolContext(Config.Debug.DB.PoolSize, "dbVerticles.dispatcher")
    }

    val dataSource get() = vertx.getSharedDataSource()
    @Suppress("UNCHECKED_CAST")
    val pk get() = table.primaryKey.fields.first() as Field<Any>

    val createAddress = Address.DB.create(entityName)
    val updateAddress = Address.DB.update(entityName)
    val readAddress = Address.DB.read(entityName)
    val findAddress = Address.DB.find(entityName)
    val deleteAddress = Address.DB.delete(entityName)


    override fun start() {
        val eb = vertx.eventBus()
        eb.consumer<JsonObject>(createAddress) { handleCreate(it) }
        eb.consumer<JsonObject>(updateAddress) { handleUpdate(it) }
        eb.consumer<JsonObject>(readAddress) { handleRead(it) }
        eb.consumer<JsonObject>(findAddress) { handleFind(it) }
        eb.consumer<JsonObject>(deleteAddress) { handleDelete(it) }
    }

    protected suspend fun <T> db(body: DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use(body) }

    protected suspend fun <T> dbAsync(body: suspend DSLContext.() -> T) =
            launch(DBPool) { jooq(dataSource).use { it.body() } }

    private fun DSLContext.selectById(id: Any) =
            select().from(table).where(pk.eq(id)).fetch().into(JsonObject::class.java).firstOrNull()

    open fun handleDelete(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.info("Delete requested for id = $id in table ${table.name}")

        val resp = db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
        }
        message.reply(resp)
    }.ignore()

    open fun handleRead(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.info("Read requested for id = $id in table ${table.name}")
        val resp = db { selectById(id) }
        message.reply(resp)
    }.ignore()

    open fun handleFind(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val query = message.body().toRecord(table.recordType.kotlin)
        log.info("Find requested in table ${table.name}:\n" +
                query.toJson().encodePrettily())

        val queryFields = table.fields().asSequence().filter { message.body().containsKey(it.name) }
        val wherePart = queryFields.map { (it as Field<Any?>).eq(query.get(it)) }.reduce(Condition::and)
        val resp = db {
            selectFrom(table)
                    .where(wherePart)
                    .fetch()
                    .into(JsonObject::class.java)
                    .let(::JsonArray)
        }
        message.reply(resp)
    }.ignore()

    open fun handleUpdate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.info("Update requested for id = $id in table ${table.name}:\n" +
                message.body().encodePrettily())
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

    open fun handleCreate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        log.info("Create requested in table ${table.name}:\n" +
                message.body().encodePrettily())
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

abstract class DatabaseVerticleWithReferences<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    override fun start() {
        super.start()

        val eb = vertx.eventBus()

        for (reference in table.references) {
            eb.consumer<JsonObject>(
                    addressFor(reference),
                    handlerFor(reference)
            )
        }
    }

    internal fun addressFor(fk: ForeignKey<R, *>) =
            "$readAddress.for.${fk.key.table.name.toLowerCase()}"

    internal fun handlerFor(fk: ForeignKey<R, *>) = { msg: Message<JsonObject> ->
        launch(UnconfinedWithExceptions(msg)) {
            @Suppress("UNCHECKED_CAST")
            val fkField = fk.fields.first() as Field<Any>

            val id = msg.body().getValue(fkField.name)

            log.info("Joined read requested for id = $id in table ${table.name} " +
                    "on key ${fkField.name}")

            dbAsync {
                val res =
                        select(*table.fields())
                                .from(table.join(fk.key.table).onKey())
                                .where(fkField.eq(id))
                                .fetchKAsync()
                                .into(JsonObject::class.java)
                                .let(::JsonArray)

                msg.reply(res)
            }
        }.ignore()
    }
}

class DebugVerticle : DatabaseVerticle<DebugRecord>(Tables.DEBUG)

class DenizenVerticle : DatabaseVerticle<DenizenRecord>(Tables.DENIZEN)

class SubmissionCommentVerticle : DatabaseVerticleWithReferences<SubmissioncommentRecord>(Tables.SUBMISSIONCOMMENT)

class CourseVerticle : DatabaseVerticle<CourseRecord>(Tables.COURSE) {

    suspend fun verify(json: JsonObject): Boolean {

        return true

    }

    override fun handleCreate(message: Message<JsonObject>) {
        launch(UnconfinedWithExceptions(message)) {
            if (verify(message.body())) {
                super.handleCreate(message)
            }
        }
    }
}

class ProjectVerticle : DatabaseVerticleWithReferences<ProjectRecord>(Tables.PROJECT)
