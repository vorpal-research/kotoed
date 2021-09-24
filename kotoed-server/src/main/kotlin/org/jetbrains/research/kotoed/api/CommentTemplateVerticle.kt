package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.CountResponse
import org.jetbrains.research.kotoed.data.api.SearchQuery
import org.jetbrains.research.kotoed.data.db.setPageForQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CommentTemplateRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord

@AutoDeployable
class CommentTemplateVerticle: AbstractKotoedVerticle() {

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.Create)
    suspend fun handleCreate(request: CommentTemplateRecord) = dbCreateAsync(request)

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.Delete)
    suspend fun handleDelete(request: CommentTemplateRecord) = dbDeleteAsync(request)

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.Update)
    suspend fun handleUpdate(request: CommentTemplateRecord) = dbUpdateAsync(request)

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.ReadAll)
    suspend fun handleReadAll(request: CommentTemplateRecord) = dbFindAsync(request)


    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.Search)
    suspend fun handleSearch(query: SearchQuery): List<CommentTemplateRecord> =
            dbQueryAsync(Tables.COMMENT_TEMPLATE) {
                setPageForQuery(query)
                filter("denizen_id == %s and (text iContains %s or name iContains %s)"
                        .formatToQuery(query.denizenId, query.text, query.text))
            }.map {
                it.toRecord<CommentTemplateRecord>()
            }

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): CountResponse =
            dbCountAsync(Tables.COMMENT_TEMPLATE) {
                filter("denizen_id == %s and (text iContains %s or name iContains %s)"
                        .formatToQuery(query.denizenId, query.text, query.text))
            }
}