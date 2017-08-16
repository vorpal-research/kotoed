package org.jetbrains.research.kotoed.db.processors

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.api.bang
import org.jetbrains.research.kotoed.db.DatabaseVerticle
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.ForeignKey
import org.jooq.Table
import org.jooq.UpdatableRecord
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

abstract class ProcessorVerticle<R : UpdatableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val processAddress = Address.DB.process(entityName)
    val verifyAddress = Address.DB.verify(entityName)

    val cache: Cache<Int, VerificationData> = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES) // TODO: Move to settings
            .build()

    val cacheMap: ConcurrentMap<Int, VerificationData>
        get() = cache.asMap()

    @JsonableEventBusConsumerForDynamic(addressProperty = "processAddress")
    suspend fun handleProcess(msg: JsonObject): VerificationData {
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Handling process for: $msg")
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
        process(data)
        return cache[id].bang()
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "verifyAddress")
    suspend fun handleVerify(msg: JsonObject): VerificationData {
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Handling verify for: $msg")
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
        return cache[id].bang()
    }

    suspend fun process(data: JsonObject?) {
        data ?: throw IllegalArgumentException("data is null")

        val id = data[pk.name] as Int

        val oldData = cache[id].bang()

        log.trace("Processing: $data")
        log.trace("Old data: $oldData")

        when (oldData.status) {
            VerificationStatus.Processed, VerificationStatus.NotReady -> {
                // do nothing
            }
            else -> {

                val notReady = oldData.copy(status = VerificationStatus.NotReady)

                val ok = cacheMap.replace(id, oldData, notReady)

                if (ok) {

                    val prereqVerificationData = checkPrereqs(data)

                    log.trace("Prereqs: $prereqVerificationData")

                    if (prereqVerificationData.all { VerificationStatus.Processed == it.status }) {

                        log.trace("Going in!")

                        cacheMap.replace(id, notReady, doProcess(data))

                    } else if (prereqVerificationData.any { VerificationStatus.Invalid == it.status }) {

                        log.trace("Some prereqs failed!")

                        cacheMap.replace(id, notReady,
                                prereqVerificationData
                                        .filter { VerificationStatus.Invalid == it.status }
                                        .reduce { acc, datum -> acc.copy(errors = acc.errors + datum.errors) }
                        )

                    } else {

                        log.trace("Wat???")

                        cacheMap.replace(id, notReady, VerificationData.Unknown)

                    }
                } else { // retry

                    log.trace("Come again?")

                    process(data)
                }
            }
        }
    }

    suspend open fun verify(data: JsonObject?): VerificationData =
            VerificationData.Processed

    suspend open fun doProcess(data: JsonObject): VerificationData =
            VerificationData.Processed

    open val checkedReferences: List<ForeignKey<R, *>> get() = table.references

    suspend open fun checkPrereqs(data: JsonObject): List<VerificationData> {

        val eb = vertx.eventBus()

        return checkedReferences
                .asSequence()
                // XXX: we expect here that we have no composite foreign keys
                .filter { it.fieldsArray.size == 1 }
                .filter { it.key.fieldsArray.size == 1 }
                .map { fkey ->
                    val from = fkey.fieldsArray.first()
                    Pair(fkey.key, data[from])
                }
                .filter { it.second != null }
                .mapTo(mutableListOf<VerificationData>()) { (rkey, id) ->
                    val to = rkey.fieldsArray.first()
                    val toTable = rkey.table

                    eb.trySendAsync(Address.DB.verify(toTable.name), JsonObject(to.name to id))
                            ?.body()
                            ?.toJsonable()
                            ?: VerificationData.Processed
                }
    }

}

