package org.jetbrains.research.kotoed.db.processors

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.launch
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
) : DatabaseVerticle<R>(table, entityName), Loggable {

    val processAddress = Address.DB.process(entityName)
    val verifyAddress = Address.DB.verify(entityName)
    val cleanAddress = Address.DB.clean(entityName)

    private val cache: Cache<Int, VerificationData> = CacheBuilder.newBuilder()
            .expireAfterAccess(Config.Processors.CacheExpiration, TimeUnit.MINUTES)
            .build()

    private val cacheMap: ConcurrentMap<Int, VerificationData>
        get() = cache.asMap()

    private suspend fun verifyIfNeeded(id: Int, data: JsonObject?) {
        val oldStatus = cacheMap.putIfAbsent(id, VerificationData.Unknown).bang()
        if (VerificationStatus.Unknown == oldStatus.status) {
            val newStatus = verify(data)
            cacheMap.replace(id, oldStatus, newStatus)
        }
        log.trace("Old status: $oldStatus")
        log.trace("New status: ${cache[id].bang()}")
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "processAddress")
    suspend fun handleProcess(msg: JsonObject): VerificationData {
        log.trace("Handling process for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        return cache[id].bang().also {
            launch(LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
                verifyIfNeeded(id, data)
                process(data)
            }
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "verifyAddress")
    suspend fun handleVerify(msg: JsonObject): VerificationData {
        log.trace("Handling verify for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        return cache[id].bang().also {
            launch(LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
                verifyIfNeeded(id, data)
            }
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "cleanAddress")
    suspend fun handleClean(msg: JsonObject): VerificationData {
        log.trace("Handling clean for: $msg")
        val id: Int by msg.delegate
        val data = db { selectById(id) }?.toJson()
        return cache[id].bang().also {
            launch(LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
                verifyIfNeeded(id, data)
                clean(data)
            }
        }
    }

    private inline suspend fun withNotReady(id: Int,
                                            oldData: VerificationData,
                                            body: () -> VerificationData): Boolean {
        when(oldData.status) {
            VerificationStatus.NotReady -> return true
            else -> {}
        }

        val notReady = oldData.copy(status = VerificationStatus.NotReady)

        val ok = cacheMap.replace(id, oldData, notReady)
        var newData: VerificationData = oldData

        try {
            if (ok) {
                newData = body()
                return true
            }
            return false
        } finally {
            cacheMap.replace(id, notReady, newData)
        }
    }

    suspend fun process(data: JsonObject?) {
        data ?: throw IllegalArgumentException("Cannot process null submission")

        val eb = vertx.eventBus()

        val id = data[pk.name] as Int

        val oldData = cache[id].bang()

        log.trace("Processing: $data")
        log.trace("Old data: $oldData")

        when(oldData.status) {
            VerificationStatus.Processed -> return
            else -> {}
        }

        withNotReady(id, oldData) {
            val prereqVerificationData = checkPrereqs(data)

            log.trace("Prereqs: $prereqVerificationData")

            when {
                prereqVerificationData.all { (_, data) -> VerificationStatus.Processed == data.status } -> {
                    log.trace("Going in!")
                    doProcess(data)
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

                    VerificationData.Invalid(failedIds)
                }

                else -> {
                    log.trace("Wat???")
                    VerificationData.Unknown
                }

            } // when
        }.let {
            if(!it) {
                log.trace("Come again?")
                vertx.delayAsync(1000)
                process(data)
            }
        }
    }

    suspend fun clean(data: JsonObject?) {
        data ?: throw IllegalArgumentException("Cannot clean null submission")

        val id = data[pk.name] as Int

        val oldData = cache[id].bang()

        log.trace("Cleaning: $data")
        log.trace("Old data: $oldData")

        val ok = withNotReady(id, oldData) {
            log.trace("Going in!")
            doClean(data)
        }
        if(ok) return

        log.trace("Come again?")
        vertx.delayAsync(1000)
        clean(data)
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
