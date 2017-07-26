package org.jetbrains.research.kotoed.db

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.*

abstract class DatabaseVerticle<R : UpdatableRecord<R>>(
        val table: Table<R>,
        val entityName: String = table.name.toLowerCase()
) : AbstractKotoedVerticle(), Loggable {

    companion object {
        val DBPool =
                newFixedThreadPoolContext(Config.Debug.Database.PoolSize, "dbVerticles.dispatcher")
    }

    val dataSource get() = vertx.getSharedDataSource()
    @Suppress("UNCHECKED_CAST")
    val pk get() = table.primaryKey.fields.first() as Field<Any>

    protected suspend fun <T> db(body: DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use(body) }

    protected suspend fun <T> dbAsync(body: suspend DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use { it.body() } }

    protected fun DSLContext.selectById(id: Any) =
            select().from(table).where(pk.eq(id)).fetch().into(JsonObject::class.java).firstOrNull()

}

abstract class CrudDatabaseVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val createAddress = Address.DB.create(entityName)
    val updateAddress = Address.DB.update(entityName)
    val readAddress = Address.DB.read(entityName)
    val findAddress = Address.DB.find(entityName)
    val deleteAddress = Address.DB.delete(entityName)

    override fun start(startFuture: Future<Void>) {
        val eb = vertx.eventBus()
        eb.consumer<JsonObject>(createAddress) { handleCreate(it) }
        eb.consumer<JsonObject>(updateAddress) { handleUpdate(it) }
        eb.consumer<JsonObject>(readAddress) { handleRead(it) }
        eb.consumer<JsonObject>(findAddress) { handleFind(it) }
        eb.consumer<JsonObject>(deleteAddress) { handleDelete(it) }
        super.start(startFuture)
    }

    open fun handleDelete(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.trace("Delete requested for id = $id in table ${table.name}")

        val resp = db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetchOne()
                    ?.into(JsonObject::class.java)
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
        message.reply(resp)
    }.ignore()

    open fun handleRead(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.trace("Read requested for id = $id in table ${table.name}")
        val resp = db { selectById(id) } ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        message.reply(resp)
    }.ignore()

    open fun handleFind(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val query = message.body().toRecord(table.recordType.kotlin)
        log.trace("Find requested in table ${table.name}:\n" +
                query.toJson().encodePrettily())

        val queryFields = table.fields().asSequence().filter { message.body().containsKey(it.name) }
        @Suppress("UNCHECKED_CAST")
        val wherePart = queryFields.map { (it as Field<Any?>).eq(query.get(it)) }.reduce(Condition::and)
        val resp = db {
            selectFrom(table)
                    .where(wherePart)
                    .fetch()
                    .into(JsonObject::class.java)
                    .let(::JsonArray)
        }
        log.trace("Found ${resp.size()} records")
        message.reply(resp)
    }.ignore()

    open fun handleUpdate(message: Message<JsonObject>) = launch(UnconfinedWithExceptions(message)) {
        val id = message.body().getValue(pk.name)
        log.trace("Update requested for id = $id in table ${table.name}:\n" +
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
        val mbody = message.body()
        log.trace("Create requested in table ${table.name}:\n" +
                mbody.encodePrettily())

        for (field in table.primaryKey.fieldsArray) {
            mbody.remove(field.name)
        }

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

abstract class CrudDatabaseVerticleWithReferences<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : CrudDatabaseVerticle<R>(table, entityName) {

    override fun start(startFuture: Future<Void>) {
        val eb = vertx.eventBus()

        table.references
                .forEach {
                    eb.consumer<JsonObject>(addressFor(it), handlerFor(it))
                }

        super.start(startFuture)
    }

    internal fun addressFor(fk: ForeignKey<R, *>) =
            "$readAddress.for.${fk.key.table.name.toLowerCase()}"

    internal fun handlerFor(fk: ForeignKey<R, *>) = { msg: Message<JsonObject> ->
        launch(UnconfinedWithExceptions(msg)) {
            @Suppress("UNCHECKED_CAST")
            val fkField = fk.fields.first() as Field<Any>

            val id = msg.body().getValue(fkField.name)

            log.trace("Joined read requested for id = $id in table ${table.name} " +
                    "on key ${fkField.name}")

            dbAsync {
                val res =
                        select(*table.fields())
                                .from(table.join(fk.key.table).onKey(fk))
                                .where(fkField.eq(id))
                                .fetchKAsync()
                                .into(JsonObject::class.java)
                                .let(::JsonArray)

                log.trace("Found ${res.size()} records")

                msg.reply(res)
            }
        }.ignore()
    }
}

@AutoDeployable
class DebugVerticle : CrudDatabaseVerticle<DebugRecord>(Tables.DEBUG)

@AutoDeployable
class DenizenVerticle : CrudDatabaseVerticle<DenizenRecord>(Tables.DENIZEN)

//@AutoDeployable
//class SubmissionVerticle : CrudDatabaseVerticleWithReferences<SubmissionRecord>(Tables.SUBMISSION)

@AutoDeployable
class SubmissionStatusVerticle : CrudDatabaseVerticleWithReferences<SubmissionStatusRecord>(Tables.SUBMISSION_STATUS)

@AutoDeployable
class SubmissionCommentVerticle : CrudDatabaseVerticleWithReferences<SubmissionCommentRecord>(Tables.SUBMISSION_COMMENT)

@AutoDeployable
class CourseVerticle : CrudDatabaseVerticle<CourseRecord>(Tables.COURSE)

@AutoDeployable
class CourseStatusVerticle : CrudDatabaseVerticleWithReferences<CourseStatusRecord>(Tables.COURSE_STATUS)

@AutoDeployable
class ProjectVerticle : CrudDatabaseVerticleWithReferences<ProjectRecord>(Tables.PROJECT)

@AutoDeployable
class ProjectStatusVerticle : CrudDatabaseVerticleWithReferences<ProjectStatusRecord>(Tables.PROJECT_STATUS)

@AutoDeployable
class OAuthProviderVerticle : CrudDatabaseVerticle<OauthProviderRecord>(Tables.OAUTH_PROVIDER)

@AutoDeployable
class OAuthProfileVerticle : CrudDatabaseVerticleWithReferences<OauthProfileRecord>(Tables.OAUTH_PROFILE)
