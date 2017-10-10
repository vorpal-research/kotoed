package org.jetbrains.research.kotoed.api

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

}