package org.jetbrains.research.kotoed.util.database

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.database.Public
import org.jetbrains.research.kotoed.util.*
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DefaultDataType
import org.jooq.impl.DefaultRecordMapper
import org.jooq.impl.SQLDataType
import org.jooq.tools.jdbc.JDBCUtils
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object JsonConverter : Converter<Any?, Any?> {
    override fun toType(): Class<Any?> {
        return Any::class.java.uncheckedCast()
    }

    override fun fromType(): Class<Any?> {
        return Any::class.java.uncheckedCast()
    }

    override fun from(databaseObject: Any?): Any? {
        // this is not very good, but vertx json api sucks
        databaseObject ?: return null

        return JsonEx.decode("$databaseObject")
    }

    override fun to(userObject: Any?): Any? {
        userObject ?: return null
        return Json.encode(userObject)
    }
}

object NoConverter : Converter<Any?, Any?> {
    override fun toType(): Class<Any?> {
        return Any::class.java.uncheckedCast()
    }

    override fun fromType(): Class<Any?> {
        return Any::class.java.uncheckedCast()
    }

    override fun from(databaseObject: Any?): Any? {
        // this is not very good, but vertx json api sucks
        databaseObject ?: return null

        return "$databaseObject"
    }

    override fun to(userObject: Any?): Any? {
        userObject ?: return null
        return "$userObject"
    }
}

interface PostgresBindingBase : Binding<Any?, Any?> {
    val typename: String

    override fun get(ctx: BindingGetStatementContext<Any?>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()))
    }

    override fun get(ctx: BindingGetResultSetContext<Any?>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    override fun get(ctx: BindingGetSQLInputContext<Any?>?) {
        throw SQLFeatureNotSupportedException()
    }

    override fun sql(ctx: BindingSQLContext<Any?>) {
        ctx.render().visit(DSL.`val`(ctx.convert(converter()).value())).sql("::$typename")
    }

    override fun set(ctx: BindingSetSQLOutputContext<Any?>?) {
        throw SQLFeatureNotSupportedException()
    }

    override fun set(ctx: BindingSetStatementContext<Any?>) {
        ctx.statement().setString(ctx.index(), ctx.convert(converter()).value().toString())
    }

    override fun register(ctx: BindingRegisterContext<Any?>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
    }
}

class PostgresJSONBBinding : PostgresBindingBase {
    override fun converter() = JsonConverter
    override val typename = "jsonb"
}

class PostgresJSONBinding : PostgresBindingBase {
    override fun converter() = JsonConverter
    override val typename = "json"
}

class PostgresTSVectorBinding : PostgresBindingBase {
    override fun converter() = NoConverter
    override val typename = "tsvector"
}

data class WrappedRecord(val table: Table<*>, val record: Record): Record by record, Jsonable {
    override fun toJson() =
            record.toJson().apply { this["table"] = table.name }

    companion object: JsonableCompanion<WrappedRecord> {
        override val dataklass = WrappedRecord::class
        override fun fromJson(json: JsonObject): WrappedRecord? {
            val table = Public.PUBLIC.tables.find { it.name == json["table"] }
            table ?: return null
            val record = json.toRecord(table.recordType.kotlin)
            record ?: return null
            return WrappedRecord(table, record)
        }
    }
}

fun<R: Record> R.toJson(): JsonObject =
        JsonObject().apply {
            for (field in fields()) {
                val fieldVal = field.getValue(this@toJson)
                fieldVal?.let { put(field.name, jsonValue(it)) }
            }
        }

fun<R: Record> JsonObject.toRecord(klazz: KClass<R>): R =
        klazz.createInstance().apply { from(this@toRecord.map) }

inline fun<reified R: Record> JsonObject.toRecord() = toRecord(R::class)

val postgresRecordMappers: RecordMapperProvider =
        object : RecordMapperProvider {
            override fun <R : Record, E : Any> provide(recordType: RecordType<R>, clazz: Class<out E>): RecordMapper<R, E> =
                    run {
                        if (clazz == JsonObject::class.java)
                            RecordMapper { record: R -> record.toJson().uncheckedCast<E>() }
                        else DefaultRecordMapper(recordType, clazz)
                    }
        }

object PostgresDataTypeEx {
    val JSONB = DefaultDataType(SQLDialect.POSTGRES_9_5, SQLDataType.OTHER, "jsonb")
            .asConvertedDataType(PostgresJSONBBinding())
}

fun jooq(ds: KotoedDataSource) =
        DSL.using(ds, JDBCUtils.dialect(ds.url)).apply { configuration().set(postgresRecordMappers) }
