@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.research.kotoed.util.database

import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DefaultRecordMapper
import org.jooq.impl.SQLDataType
import org.jooq.tools.jdbc.JDBCUtils
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types

object JsonConverter: Converter<Any?, Any?> {
    override fun toType(): Class<Any?> { return Any::class.java as Class<Any?> }
    override fun fromType(): Class<Any?> { return Any::class.java as Class<Any?> }

    override fun from(databaseObject: Any?): Any? {
        // this is not very good, but vertx json api sucks
        databaseObject ?: return null

        val fakeElement = "fakeElement"
        return JsonObject("""{ "$fakeElement" : $databaseObject }""").getValue(fakeElement)
    }

    override fun to(userObject: Any?): Any? {
        userObject ?: return null
        return Json.encode(userObject)
    }
}

object PostgresJSONBinding: Binding<Any?, Any?> {
    override fun converter() = JsonConverter

    override fun get(ctx: BindingGetStatementContext<Any?>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }

    override fun get(ctx: BindingGetResultSetContext<Any?>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    override fun get(ctx: BindingGetSQLInputContext<Any?>?) {
        throw SQLFeatureNotSupportedException()
    }

    override fun sql(ctx: BindingSQLContext<Any?>) {
        ctx.render().visit(DSL.`val`(ctx.convert(converter()).value())).sql("::jsonb")
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

val jsonRecordMappers: RecordMapperProvider =
        object: RecordMapperProvider {
            override fun <R : Record, E : Any> provide(recordType: RecordType<R>, clazz: Class<out E>): RecordMapper<R, E> =
                    run {
                            if (clazz == JsonObject::class.java)
                                RecordMapper { record: R ->
                                    JsonObject().apply {
                                        for (field in record.fields()) {
                                            put(field.name, field.getValue(record))
                                        }
                                    } as E
                                }
                            else DefaultRecordMapper(recordType, clazz)
                        }
        }

object PostgresDataTypeEx {
    val JSONB = SQLDataType.VARCHAR.asConvertedDataType(PostgresJSONBinding)
}

fun jooq(ds: KotoedDataSource) = DSL.using(ds, JDBCUtils.dialect(ds.url)).apply{ configuration().set(jsonRecordMappers) }
