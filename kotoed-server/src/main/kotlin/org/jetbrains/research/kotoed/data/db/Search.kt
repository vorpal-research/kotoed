package org.jetbrains.research.kotoed.data.db

import io.vertx.core.json.JsonObject
import kotlinx.Warnings.NOTHING_TO_INLINE
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableRecord
import kotlin.coroutines.experimental.buildSequence

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(query: ComplexDatabaseQuery) = defaultField(query.table!!)
@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(table: String) = "${table}_id"
@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultResultField(field: String) = field.replace("_id", "")

data class DatabaseJoin(
        val query: ComplexDatabaseQuery? = null,
        val field: String? = query?.table?.let(::defaultField),
        val resultField: String? = field?.let(::defaultResultField),
        val key: String? = null // null means pk
): Jsonable {
    fun fillDefaults(): DatabaseJoin {
        val query_ = (query ?: ComplexDatabaseQuery()).fillDefaults()
        val field_ = field ?: query_.table?.let(::defaultField)
        val resultField_ = resultField ?: field?.let(::defaultResultField)
        return copy(field = field_, resultField = resultField_, query = query_)
    }
}

fun DatabaseJoin(table: String,
                 field: String = defaultField(table),
                 resultField: String = defaultResultField(field),
                 key: String? = null) =
        DatabaseJoin(ComplexDatabaseQuery(table), field, resultField, key)

data class ComplexDatabaseQuery(
        val table: String? = null,
        val find: JsonObject? = JsonObject(),
        val joins: List<DatabaseJoin>? = listOf(),
        val filter: String? = null,
        val limit: Int? = null,
        val offset: Int? = null
): Jsonable {
    fun join(table: String,
             field: String = defaultField(table),
             resultField: String = defaultResultField(field),
             key: String? = null) =
            copy(joins = (joins ?: listOf()) + DatabaseJoin(table, field, resultField, key))

    fun<R: Record> join(table: Table<R>,
             field: String = defaultField(table.name),
             resultField: String = defaultResultField(field),
             key: String? = null) = join(table.name, field, resultField, key)


    fun join(query: ComplexDatabaseQuery,
             field: String? = query.table?.let(::defaultField),
             resultField: String? = field?.let(::defaultResultField),
             key: String? = null) =
            copy(joins = (joins ?: listOf()) + DatabaseJoin(query, field, resultField, key))

    fun find(record: Record) = copy(find = record.toJson())

    fun fillDefaults(): ComplexDatabaseQuery {
        val find_ = find ?: JsonObject()
        val joins_ = (joins ?: listOf()).map { it.fillDefaults() }
        return copy(find = find_, joins = joins_)
    }

    fun filter(filter: String) = copy(filter = filter)

    fun limit(limit: Int) = copy(limit = limit)
    fun offset(offset: Int) = copy(offset = offset)

}

fun <R: TableRecord<R>> ComplexDatabaseQuery(find: R) =
        ComplexDatabaseQuery(table = find.table.name, find = find.toJson())

fun <R: Record> ComplexDatabaseQuery(table: Table<R>) =
        ComplexDatabaseQuery(table = table.name)

data class TextSearchRequest(val text: String): Jsonable

