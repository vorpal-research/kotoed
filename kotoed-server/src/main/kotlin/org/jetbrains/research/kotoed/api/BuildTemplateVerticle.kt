package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.BuildTemplate
import org.jetbrains.research.kotoed.database.tables.records.BuildTemplateRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@AutoDeployable
class BuildTemplateVerticle : AbstractKotoedVerticle() {

    @JsonableEventBusConsumerFor(Address.Api.BuildTemplate.Read)
    suspend fun handleRead(bt: BuildTemplateRecord): BuildTemplate {
        val res: BuildTemplateRecord = dbFetchAsync(bt)
        return BuildTemplate.ofRecord(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.BuildTemplate.Create)
    suspend fun handleCreate(bt: BuildTemplate): BuildTemplate {
        val record = bt.toRecord()
        record.reset("id")
        val res: BuildTemplateRecord = dbCreateAsync(record)
        return BuildTemplate.ofRecord(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.BuildTemplate.Update)
    suspend fun handleUpdate(bt: BuildTemplate): BuildTemplate {
        val res: BuildTemplateRecord = dbUpdateAsync(bt.toRecord())
        return BuildTemplate.ofRecord(res)
    }

}