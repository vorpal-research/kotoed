package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.research.kotoed.util.JsonObject
import org.jetbrains.research.kotoed.data.api.SearchQuery
import org.jetbrains.research.kotoed.database.tables.records.CommentTemplateRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor

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
    suspend fun handleSearch(query: SearchQuery): List<CommentTemplateRecord> {
        val pageSize = query.pageSize ?: Int.MAX_VALUE
        val currentPage = query.currentPage ?: 0

        val res = dbFindAsync(CommentTemplateRecord().apply {
            denizenId = query.denizenId
        }).filter { query.text.toLowerCase() in it.text.toLowerCase()
                || query.text.toLowerCase() in it.name.toLowerCase() }
                .sortedBy { it.id }
                .drop(currentPage * pageSize)
                .take(pageSize)

        return res
    }

    @JsonableEventBusConsumerFor(Address.Api.CommentTemplate.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): JsonObject =
            JsonObject("count" to handleSearch(query).size)

}