package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerForDynamic
import org.jetbrains.research.kotoed.util.database.toJson

@AutoDeployable
class DenizenVerticle : CrudDatabaseVerticle<DenizenRecord>(Tables.DENIZEN) {
    val fullAddress = Address.DB.full("denizen")

    @JsonableEventBusConsumerForDynamic(addressProperty = "fullAddress")
    suspend fun handleFull(query: DenizenRecord): JsonObject {
        return query.toJson()
    }
}
