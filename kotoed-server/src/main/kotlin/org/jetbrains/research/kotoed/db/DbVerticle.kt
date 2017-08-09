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

abstract class DatabaseVerticle<R : TableRecord<R>>(
        val table: Table<R>,
        val entityName: String = table.name.toLowerCase()
) : AbstractKotoedVerticle(), Loggable {

    companion object {
        val DBPool =
                newFixedThreadPoolContext(Config.Debug.Database.PoolSize, "dbVerticles.dispatcher")
    }

    val dataSource get() = vertx.getSharedDataSource()

    val pk: Field<Any>
        get() = table.primaryKey?.fields?.first()?.uncheckedCast<Field<Any>>()
                ?: table.field("id").uncheckedCast<Field<Any>>()

    protected suspend fun <T> db(body: DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use(body) }

    protected suspend fun <T> dbAsync(body: suspend DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use { it.body() } }

    protected fun DSLContext.selectById(id: Any) =
            select().from(table).where(pk.eq(id)).fetch().into(JsonObject::class.java).firstOrNull()

}

abstract class CrudDatabaseVerticle<R : TableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val createAddress = Address.DB.create(entityName)
    val updateAddress = Address.DB.update(entityName)
    val readAddress = Address.DB.read(entityName)
    val findAddress = Address.DB.find(entityName)
    val deleteAddress = Address.DB.delete(entityName)

    @JsonableEventBusConsumerForDynamic(addressProperty = "deleteAddress")
    suspend fun handleDeleteWrapper(message: JsonObject) = handleDelete(message)
    protected open suspend fun handleDelete(message: JsonObject): JsonObject {
        val id = message.getValue(pk.name)
        log.trace("Delete requested for id = $id in table ${table.name}")

        return db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into<JsonObject>()
                    .firstOrNull()
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "readAddress")
    suspend fun handleReadWrapper(message: JsonObject) = handleRead(message)
    protected open suspend fun handleRead(message: JsonObject): JsonObject {
        val id = message.getValue(pk.name)
        log.trace("Read requested for id = $id in table ${table.name}")
        return db { selectById(id) } ?: throw NotFound("Cannot find ${table.name} entry for id $id")
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "findAddress")
    suspend fun handleFindWrapper(message: JsonObject) = handleFind(message)
    protected open suspend fun handleFind(message: JsonObject): JsonArray {
        val query = message.toRecord(table.recordType.kotlin)
        log.trace("Find requested in table ${table.name}:\n" +
                query.toJson().encodePrettily())

        val queryFields = table
                .fields()
                .asSequence()
                .filter { message.containsKey(it.name) }
                .map { it.uncheckedCast<Field<Any>>() }
        val wherePart = queryFields.map { it.eq(query.get(it)) }.reduce(Condition::and)
        val resp = db {
            selectFrom(table)
                    .where(wherePart)
                    .fetch()
                    .into(JsonObject::class.java)
                    .let(::JsonArray)
        }
        log.trace("Found ${resp.size()} records")
        return resp
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "updateAddress")
    suspend fun handleUpdateWrapper(message: JsonObject) = handleUpdate(message)
    protected open suspend fun handleUpdate(message: JsonObject): JsonObject {
        val id = message.getValue(pk.name)
        log.trace("Update requested for id = $id in table ${table.name}:\n" +
                message.encodePrettily())
        return db {
            update(table)
                    .set(newRecord(table, message.map))
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into<JsonObject>()
                    .firstOrNull()
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "createAddress")
    suspend fun handleCreateWrapper(message: JsonObject) = handleCreate(message)
    protected open suspend fun handleCreate(message: JsonObject): JsonObject {
        log.trace("Create requested in table ${table.name}:\n" +
                message.encodePrettily())

        for (field in table.primaryKey.fieldsArray) {
            message.remove(field.name)
        }

        return db {
            insertInto(table)
                    .set(newRecord(table, message.map))
                    .returning()
                    .fetch()
                    .into<JsonObject>()
                    .first()
        }
    }

}

abstract class CrudDatabaseVerticleWithReferences<R : TableRecord<R>>(
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
            val fkField = fk.fields.first().uncheckedCast<Field<Any>>()

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
class DenizenUnsafeVerticle : CrudDatabaseVerticle<DenizenUnsafeRecord>(Tables.DENIZEN_UNSAFE)

@AutoDeployable
class DenizenVerticle : CrudDatabaseVerticle<DenizenRecord>(Tables.DENIZEN)

//@AutoDeployable
//class SubmissionVerticle : CrudDatabaseVerticleWithReferences<SubmissionRecord>(Tables.SUBMISSION)

@AutoDeployable
class SubmissionStatusVerticle : CrudDatabaseVerticleWithReferences<SubmissionStatusRecord>(Tables.SUBMISSION_STATUS)

@AutoDeployable
class BuildVerticle : CrudDatabaseVerticleWithReferences<BuildRecord>(Tables.BUILD)

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

@AutoDeployable
class NotificationVerticle : CrudDatabaseVerticleWithReferences<NotificationRecord>(Tables.NOTIFICATION)
