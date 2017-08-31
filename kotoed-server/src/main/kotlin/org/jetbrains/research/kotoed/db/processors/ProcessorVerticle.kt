package org.jetbrains.research.kotoed.db.processors

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
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
    val cleanAddress = Address.DB.clean(entityName)

    private val cache: Cache<Int, VerificationData> = CacheBuilder.newBuilder()
            .expireAfterAccess(Config.Processors.CacheExpiration, TimeUnit.MINUTES)
            .build()

    private val cacheMap: ConcurrentMap<Int, VerificationData>
        get() = cache.asMap()

    @JsonableEventBusConsumerForDynamic(addressProperty = "processAddress")
    suspend fun handleProcess(msg: JsonObject): VerificationData {
        log.trace("Handling process for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
        return cache[id].bang().also {
            launch { process(data) }
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "verifyAddress")
    suspend fun handleVerify(msg: JsonObject): VerificationData {
        log.trace("Handling verify for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
        return cache[id].bang()
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "cleanAddress")
    suspend fun handleClean(msg: JsonObject): VerificationData {
        log.trace("Handling clean for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
        return cache[id].bang().also {
            launch { clean(data) }
        }
    }

    suspend fun process(data: JsonObject?) {
        data ?: throw IllegalArgumentException("Cannot process null submission")

        val eb = vertx.eventBus()

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

                    when {

                        prereqVerificationData.all { (_, data) -> VerificationStatus.Processed == data.status } -> {
                            log.trace("Going in!")
                            cacheMap.replace(id, notReady, doProcess(data))
                        }

                        prereqVerificationData.any { (_, data) -> VerificationStatus.Invalid == data.status } -> {
                            log.trace("Some prereqs failed!")

                            // FIXME akhin: this stuff is funky

                            val failedData = prereqVerificationData
                                    .filter { (_, data) -> VerificationStatus.Invalid == data.status }

                            val failedIds = failedData.flatMap { (table, data) ->
                                data.errors.map { errorId ->
                                    val errorData = eb.trySendAsync(
                                            Address.DB.read("${table.name}_status"),
                                            JsonObject("id" to errorId))
                                            ?.body()
                                            ?.getJsonObject("data")
                                            ?: return@map -1

                                    return@map eb.trySendAsync(
                                            Address.DB.create("${entityName}_status"),
                                            JsonObject("${entityName}_id" to id, "data" to errorData))
                                            ?.body()
                                            ?.getInteger("id")
                                            ?: -1
                                }
                            }.filter { it < 0 } +
                                    if (VerificationStatus.Invalid == oldData.status)
                                        oldData.errors
                                    else
                                        emptyList()

                            cacheMap.replace(id, notReady, VerificationData.Invalid(failedIds))
                        }

                        else -> {
                            log.trace("Wat???")
                            cacheMap.replace(id, notReady, VerificationData.Unknown)
                        }

                    } // when

                } else { // retry

                    log.trace("Come again?")
                    process(data)

                }
            }
        }
    }

    suspend fun clean(data: JsonObject?) {
        data ?: throw IllegalArgumentException("Cannot clean null submission")

        val id = data[pk.name] as Int

        val oldData = cache[id].bang()

        log.trace("Cleaning: $data")
        log.trace("Old data: $oldData")

        val notReady = oldData.copy(status = VerificationStatus.NotReady)

        val ok = cacheMap.replace(id, oldData, notReady)

        if (ok) {

            log.trace("Going in!")
            cacheMap.replace(id, notReady, doClean(data))

        } else {

            log.trace("Come again?")
            clean(data)

        }
    }

    suspend open fun verify(data: JsonObject?): VerificationData =
            VerificationData.Processed

    suspend open fun doProcess(data: JsonObject): VerificationData =
            VerificationData.Processed

    suspend open fun doClean(data: JsonObject): VerificationData =
            VerificationData.Unknown

    open val checkedReferences: List<ForeignKey<R, *>> get() = table.references

    suspend open fun checkPrereqs(data: JsonObject): List<Pair<Table<*>, VerificationData>> {

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
                .mapTo(mutableListOf<Pair<Table<*>, VerificationData>>()) { (rkey, id) ->
                    val to = rkey.fieldsArray.first()
                    val toTable = rkey.table

                    toTable to (
                            eb.trySendAsync(Address.DB.verify(toTable.name), JsonObject(to.name to id))
                                    ?.body()
                                    ?.toJsonable()
                                    ?: VerificationData.Processed
                            )
                }
    }

}
