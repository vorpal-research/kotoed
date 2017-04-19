package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.db.DatabaseVerticle
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.UnconfinedWithExceptions
import org.jetbrains.research.kotoed.util.delegate
import org.jetbrains.research.kotoed.util.ignore
import org.jooq.Table
import org.jooq.UpdatableRecord

abstract class ProcessorVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val processAddress = Address.DB.process(entityName)

    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(
                processAddress,
                this::handleProcess
        )
    }

    open fun handleProcess(msg: Message<JsonObject>) = launch(UnconfinedWithExceptions(msg)) {
        val id: Int by msg.body().delegate
        process(db { selectById(id) })
    }.ignore()

    abstract suspend fun process(data: JsonObject?)

}

class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {
    suspend override fun process(data: JsonObject?) {
        data ?: return

        // TODO
    }
}
