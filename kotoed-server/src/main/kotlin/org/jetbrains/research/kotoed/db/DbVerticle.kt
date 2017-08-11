package org.jetbrains.research.kotoed.db

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.yield
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Public
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.database.*
import org.jooq.*
import org.jooq.impl.DSL
import ru.spbstu.ktuples.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.sequences.Sequence

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

    val recordClass: KClass<R> = table.recordType.kotlin.uncheckedCast()

    protected suspend fun <T> db(body: DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use(body) }

    protected suspend fun <T> dbAsync(body: suspend DSLContext.() -> T) =
            run(DBPool) { jooq(dataSource).use { it.body() } }

    protected fun DSLContext.selectById(id: Any) =
            select().from(table).where(pk.eq(id)).fetch().into(recordClass).firstOrNull()

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
    val queryAddress = Address.DB.query(entityName)


    @JsonableEventBusConsumerForDynamic(addressProperty = "deleteAddress")
    suspend fun handleDeleteWrapper(message: JsonObject) =
            handleDelete(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleDelete(message: R): R {
        val id = message.getValue(pk.name)
        log.trace("Delete requested for id = $id in table ${table.name}")

        return db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into(recordClass)
                    .firstOrNull()
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "readAddress")
    suspend fun handleReadWrapper(message: JsonObject) =
            handleRead(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleRead(message: R): R {
        val id = message.getValue(pk.name)
        log.trace("Read requested for id = $id in table ${table.name}")
        return db { selectById(id) } ?: throw NotFound("Cannot find ${table.name} entry for id $id")
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "findAddress")
    suspend fun handleFindWrapper(message: JsonObject) =
            handleFind(message.toRecord(recordClass))

    protected open suspend fun handleFind(message: R): JsonArray {
        val query = message
        log.trace("Find requested in table ${table.name}:\n" +
                query.toJson().encodePrettily())

        val queryFields = table
                .fields()
                .asSequence()
                .filter { message[it] != null }
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
    suspend fun handleUpdateWrapper(message: JsonObject) =
            handleUpdate(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleUpdate(message: R): R {
        val id = message[pk]
        log.trace("Update requested for id = $id in table ${table.name}:\n" +
                message.toJson().encodePrettily())
        return db {
            update(table)
                    .set(message)
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into(recordClass)
                    .firstOrNull()
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "createAddress")
    suspend fun handleCreateWrapper(message: JsonObject) =
            handleCreate(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleCreate(message: R): R {
        log.trace("Create requested in table ${table.name}:\n" +
                message.toJson().encodePrettily())

        for (field in table.primaryKey.fieldsArray) {
            message.reset(field)
        }

        return db {
            insertInto(table)
                    .set(message)
                    .returning()
                    .fetch()
                    .into(recordClass)
                    .first()
        }
    }

    private fun ComplexDatabaseQuery.joinSequence(tbl: Table<out Record> = tableByName(table!!)!!)
                : Sequence<Tuple5<Table<out Record>, String, Table<out Record>, JsonObject, String?>> =
            buildSequence {
                for((query, field, resultField, key) in joins!!) {
                    val qtable = when {
                        (query.table == null) -> tbl.tableReferencedBy(tbl.field(field))
                        else -> tableByName(query.table)
                    } ?: throw IllegalArgumentException("No table found for $resultField")

                    val joinedTable = qtable.`as`("${tbl.name}.$resultField")
                    yield(Tuple() + tbl + field!! + joinedTable + query.find!! + key)
                    yieldAll(query.joinSequence(joinedTable))
                }
            }

    @JsonableEventBusConsumerForDynamic(addressProperty = "queryAddress")
    suspend fun handleQueryWrapper(message: JsonObject) =
            handleQuery(message.toJsonable())

    private fun makeFindCondition(table: Table<*>, record: JsonObject): Condition {
        val queryFields = table
                .fields()
                .asSequence()
                .filter { record[it] != null }
                .map { it.uncheckedCast<Field<Any>>() }
                .toList()

        return DSL.and(queryFields.map { table.field(it).eq(record[it]) })
    }

    protected open suspend fun handleQuery(message_: ComplexDatabaseQuery): JsonArray {
        fun die(): Nothing = throw IllegalArgumentException("Illegal query")
        val message = when {
            message_.table == null -> message_.copy(table = this@CrudDatabaseVerticle.table.name)
            else -> message_
        }.fillDefaults()

        log.trace("Query in table ${table.name}:\n" +
                message.toJson().encodePrettily())

        val table = Public.PUBLIC.tables.find { it.name == message.table } ?: die()

        val joins = message.joinSequence().toList()
        val joinedTables: List<Table<*>> = joins.map { it.v2 }

        return db {
            val select = select(
                    table.fields().asList() + joinedTables.flatMap { it.fields().asList() }
            ).from(table)
            val join = joins.fold(select) {
                a, (from, field, to, _, key) ->
                    a.leftJoin(to).on(from.field(field).uncheckedCast<Field<Any>>().eq(key?.let { to.field(it) } ?: to.primaryKeyField))
            }
            val condition: Condition = joins.fold(DSL.condition(true)) {
                    a: Condition, (_, _, to, record, _) -> a.and(makeFindCondition(to, record))
            }
            val baseCondition = makeFindCondition(table, message.find!!)
            val where = join.where(condition).and(baseCondition)

            where
                    .let {
                        when {
                            message.limit != null && message.offset != null -> it.limit(message.limit).offset(message.offset)
                            message.limit != null -> it.limit(message.limit)
                            message.offset != null -> it.offset(message.offset)
                            else -> it
                        }
                    }
                    .fetch()
        }.map { record ->
            val ret = record.into(table).toJson()
            joinedTables.forEach { table ->
                ret[table.name.split('.').drop(1)] = record.into(table).toJson()
            }
            ret
        }.let(::JsonArray)
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
class SubmissionResultVerticle : CrudDatabaseVerticleWithReferences<SubmissionResultRecord>(Tables.SUBMISSION_RESULT)

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
