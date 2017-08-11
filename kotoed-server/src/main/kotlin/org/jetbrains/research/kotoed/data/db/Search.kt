package org.jetbrains.research.kotoed.data.db

import io.vertx.core.json.JsonObject
import kotlinx.Warnings.NOTHING_TO_INLINE
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.Record
import kotlin.coroutines.experimental.buildSequence

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(query: ComplexDatabaseQuery) = defaultField(query.table!!)
@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(table: String) = "${table}_id"
@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultResultField(field: String) = field.replace("_id", "")

data class DatabaseJoin(
        val query: ComplexDatabaseQuery,
        val field: String? = query.table?.let(::defaultField),
        val resultField: String? = field?.let(::defaultResultField),
        val key: String? = null // null means pk
): Jsonable {
    fun fillDefaults(): DatabaseJoin {
        val field_ = field ?: query.table?.let(::defaultField)
        val resultField_ = resultField ?: field?.let(::defaultResultField)
        val query_ = query.fillDefaults()
        return copy(field = field_, resultField = resultField_, query = query_)
    }
}

fun DatabaseJoin(table: String,
                 field: String = defaultField(table),
                 resultField: String = defaultResultField(field),
                 key: String? = null) =
        DatabaseJoin(ComplexDatabaseQuery(table), field, resultField, key)

data class ComplexDatabaseQuery(
        val table: String?,
        val find: JsonObject? = JsonObject(),
        val joins: List<DatabaseJoin>? = listOf(),
        val limit: Int? = null,
        val offset: Int? = null
): Jsonable {
    fun join(table: String,
             field: String = defaultField(table),
             resultField: String = defaultResultField(field),
             key: String? = null) =
            copy(joins = (joins ?: listOf()) + DatabaseJoin(table, field, resultField, key))

    fun find(record: Record) = copy(find = record.toJson())

    fun fillDefaults(): ComplexDatabaseQuery {
        val find_ = find ?: JsonObject()
        val joins_ = (joins ?: listOf()).map { it.fillDefaults() }
        return copy(find = find_, joins = joins_)
    }

}

data class SearchObject(val value: String? = null, val regex: Boolean? = null): Jsonable

data class SearchRequest(
        val table: String,
        val start: Int,
        val length: Int,
        val search: SearchObject
        ) : Jsonable

