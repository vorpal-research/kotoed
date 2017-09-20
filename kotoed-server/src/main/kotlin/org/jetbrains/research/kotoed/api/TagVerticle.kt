package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.database.tables.records.TagRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.use

@AutoDeployable
class TagVerticle : AbstractKotoedVerticle() {
    @JsonableEventBusConsumerFor(Address.Api.Tag.Create)
    suspend fun handleCreate(tag: TagRecord) = dbCreateAsync(tag)

    @JsonableEventBusConsumerFor(Address.Api.Tag.Read)
    suspend fun handleRead(tag: TagRecord) = dbFetchAsync(tag)

    @JsonableEventBusConsumerFor(Address.Api.Tag.Delete)
    suspend fun handleDelete(tag: TagRecord) = dbDeleteAsync(tag)

    @JsonableEventBusConsumerFor(Address.Api.Tag.List)
    suspend fun handleList(message: Unit) = dbFindAsync(TagRecord()).also {
        use(message)
    }
}
