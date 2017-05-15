package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor

@AutoDeployable
class DenizenVerticle: AbstractKotoedVerticle() {

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Create)
    suspend fun handleCreate(denizen: DenizenRecord): DbRecordWrapper =
            DbRecordWrapper(dbCreateAsync(denizen), VerificationData.Processed)

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Read)
    suspend fun handleRead(denizen: DenizenRecord): DbRecordWrapper =
            DbRecordWrapper(dbFetchAsync(denizen), VerificationData.Processed)

}