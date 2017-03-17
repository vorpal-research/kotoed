@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.research.kotoed.util.database

import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types


object PostgresJSONBinding: Binding<Any?, Any?> {
    override fun converter(): Converter<Any?, Any?> {
        return object : Converter<Any?, Any?> {
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
    }

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

object PostgresDataTypeEx {
    val JSONB = SQLDataType.VARCHAR.asConvertedDataType(PostgresJSONBinding)
}
