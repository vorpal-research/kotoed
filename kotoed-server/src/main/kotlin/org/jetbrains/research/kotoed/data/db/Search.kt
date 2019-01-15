package org.jetbrains.research.kotoed.data.db

import io.vertx.core.json.JsonObject
import kotlinx.Warnings.NOTHING_TO_INLINE
import org.intellij.lang.annotations.Language
import org.jetbrains.research.kotoed.api.Magic
import org.jetbrains.research.kotoed.data.api.PageableQuery
import org.jetbrains.research.kotoed.data.api.SearchQuery
import org.jetbrains.research.kotoed.data.api.SearchQueryWithTags
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.*

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(query: ComplexDatabaseQuery) = defaultField(query.table!!)

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultField(table: String) = "${table}_id"

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultResultField(field: String) = field.replace("_id", "")

@Suppress(NOTHING_TO_INLINE)
internal inline fun defaultReverseResultField(table: String) = table + "s"

data class DatabaseJoin(
        val query: ComplexDatabaseQuery? = null,
        val field: String? = query?.table?.let(::defaultField),
        val resultField: String? = field?.let(::defaultResultField),
        val key: String? = null // null means pk
) : Jsonable {
    fun fillDefaults(): DatabaseJoin {
        val query_ = (query ?: ComplexDatabaseQuery()).fillDefaults()
        val field_ = field ?: query_.table?.let(::defaultField)
        val resultField_ = resultField ?: field?.let(::defaultResultField)
        return copy(field = field_, resultField = resultField_, query = query_)
    }
}

data class ReverseDatabaseJoin(
        val query: ComplexDatabaseQuery?,
        val field: String?,
        val resultField: String? = query?.table?.let(::defaultReverseResultField),
        val key: String? = null // null means pk
) : Jsonable {
    fun fillDefaults(): ReverseDatabaseJoin {
        val query_ = (query ?: ComplexDatabaseQuery()).fillDefaults()
        val resultField_ = resultField ?: query?.table?.let(::defaultReverseResultField)
        return copy(resultField = resultField_, query = query_)
    }
}

fun DatabaseJoin(table: String,
                 field: String = defaultField(table),
                 resultField: String = defaultResultField(field),
                 key: String? = null) =
        DatabaseJoin(ComplexDatabaseQuery(table), field, resultField, key)

fun ReverseDatabaseJoin(table: String,
                        field: String?,
                        resultField: String = defaultReverseResultField(table),
                        key: String? = null) =
        ReverseDatabaseJoin(ComplexDatabaseQuery(table), field, resultField, key)

data class ComplexDatabaseQuery(
        val table: String? = null,
        val find: JsonObject? = JsonObject(),
        val joins: List<DatabaseJoin>? = listOf(),
        val rjoins: List<ReverseDatabaseJoin>? = listOf(),
        val filter: String? = null,
        val sortBy: List<String>? = listOf(),
        val limit: Int? = null,
        val offset: Int? = null
) : Jsonable {
    fun join(table: String,
             field: String = defaultField(table),
             resultField: String = defaultResultField(field),
             key: String? = null) =
            copy(joins = joins.orEmpty() + DatabaseJoin(table, field, resultField, key))

    fun <R : Record> join(table: Table<R>,
                          field: String = defaultField(table.name),
                          resultField: String = defaultResultField(field),
                          key: String? = null) = join(table.name, field, resultField, key)


    fun join(query: ComplexDatabaseQuery,
             field: String? = query.table?.let(::defaultField),
             resultField: String? = field?.let(::defaultResultField),
             key: String? = null) =
            copy(joins = joins.orEmpty() + DatabaseJoin(query, field, resultField, key))

    fun rjoin(table: String,
              field: String? = this.table?.let(::defaultField),
              resultField: String = defaultReverseResultField(table),
              key: String? = null) =
            copy(rjoins = rjoins.orEmpty() + ReverseDatabaseJoin(table, field, resultField, key))

    fun <R : Record> rjoin(table: Table<R>,
                           field: String? = this.table?.let(::defaultField),
                           resultField: String = defaultReverseResultField(table.name),
                           key: String? = null) = rjoin(table.name, field, resultField, key)


    fun rjoin(query: ComplexDatabaseQuery,
              field: String? = this.table?.let(::defaultField),
              resultField: String? = query.table?.let(::defaultReverseResultField),
              key: String? = null) =
            copy(rjoins = rjoins.orEmpty() + ReverseDatabaseJoin(query, field, resultField, key))

    fun find(record: Record): ComplexDatabaseQuery {
        val newFind = record.toJson()
        if (find != null) {
            newFind.mergeIn(find)
        }
        return copy(find = newFind)
    }

    fun fillDefaults(): ComplexDatabaseQuery {
        val find_ = find ?: JsonObject()
        val joins_ = joins.orEmpty().map { it.fillDefaults() }
        val rjoins_ = rjoins.orEmpty().map { it.fillDefaults() }
        val sortBy_ = sortBy.orEmpty()
        return copy(find = find_, joins = joins_, rjoins = rjoins_, sortBy = sortBy_)
    }

    fun filter(@Language("Kotlin") filter: String) = when(this.filter) {
        null -> copy(filter = filter)
        else -> copy(filter = "(${this.filter}) and ($filter)")
    }
    fun sortBy(@Language("Kotlin") expr: String) = copy(sortBy = sortBy.orEmpty() + expr)

    fun limit(limit: Int) = copy(limit = limit)
    fun offset(offset: Int) = copy(offset = offset)

}

fun ComplexDatabaseQuery.textSearch(value: String, fieldName: String = "document"): ComplexDatabaseQuery {
    if(value.isBlank()) return this
    var res = this
    val (iterms, terms) = value
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .partition { it.startsWith("!") }

    if(terms.isNotEmpty())
        res = res.filter("$fieldName matches %s".formatToQuery(terms.joinToString(" ")))
    if(iterms.isNotEmpty())
        res = res.filter(iterms.map { "!($fieldName matches %s)".formatToQuery( it.drop(1)) }.joinToString(" and "))

    return res
}

fun <R : TableRecord<R>> ComplexDatabaseQuery(find: R) =
        ComplexDatabaseQuery(table = find.table.name, find = find.toJson())

fun <R : Record> ComplexDatabaseQuery(table: Table<R>) =
        ComplexDatabaseQuery(table = table.name)

fun ComplexDatabaseQuery.setPageForQuery(query: PageableQuery): ComplexDatabaseQuery {
    // TODO maybe propagate smth like bad request error to the client
    val pageSize = query.pageSize ?: Magic.MaxPageSize
    val currentPage = query.currentPage ?: 0
    return this
            .limit(pageSize)
            .offset(currentPage * pageSize)
}

data class TextSearchRequest(val text: String) : Jsonable

@DslMarker
annotation class TypedQueryLanguage

@TypedQueryLanguage
class TypedQueryBuilder<T : TableRecord<T>>(val table: Table<T>) {
    var query: ComplexDatabaseQuery = ComplexDatabaseQuery(table.name)

    fun find(body: T.() -> Unit) {
        val rec = table.newRecord()
        rec.body()
        query = query.find(rec)
    }

    fun find(record: T) {
        query = query.find(record)
    }

    fun <U : TableRecord<U>> join(table: Table<U>,
                                  field: String = defaultField(table.name),
                                  resultField: String = defaultResultField(field),
                                  key: String? = null,
                                  body: TypedQueryBuilder<U>.() -> Unit = {}) {
        val builder = TypedQueryBuilder(table)
        builder.body()
        query = query.join(builder.query, field, resultField, key)
    }

    fun <U : TableRecord<U>> rjoin(table: Table<U>,
              field: String = defaultField(this.table.name),
              resultField: String = defaultReverseResultField(table.name),
              key: String? = null,
              body: TypedQueryBuilder<U>.() -> Unit = {}) {
        val builder = TypedQueryBuilder(table)
        builder.body()
        query = query.rjoin(builder.query, field, resultField, key)
    }

    fun limit(limit: Int) { query = query.limit(limit) }
    fun offset(offset: Int)  { query = query.offset(offset) }
    fun filter(@Language("Kotlin") filter: String) { query = query.filter(filter) }
    fun sortBy(@Language("Kotlin") expr: String) { query = query.sortBy(expr) }
}

fun <T: TableRecord<T>> TypedQueryBuilder<T>.textSearch(value: String, fieldName: String = "document") {
    query = query.textSearch(value, fieldName)
}

fun <T: TableRecord<T>> query(table: Table<T>, body: TypedQueryBuilder<T>.() -> Unit): ComplexDatabaseQuery {
    val builder = TypedQueryBuilder(table)
    builder.body()
    return builder.query.fillDefaults()
}

fun <T : TableRecord<T>> TypedQueryBuilder<T>.setPageForQuery(pq: PageableQuery) {
    query = query.setPageForQuery(pq)
}
