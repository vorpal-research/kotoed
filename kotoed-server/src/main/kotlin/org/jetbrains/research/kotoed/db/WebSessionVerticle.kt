package org.jetbrains.research.kotoed.db

import io.vertx.core.Future
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.WebSession
import org.jetbrains.research.kotoed.database.tables.records.WebSessionRecord
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.database.into

@AutoDeployable
class WebSessionVerticle : CrudDatabaseVerticle<WebSessionRecord>(Tables.WEB_SESSION) {
    private var timerId: Long = -1

    override fun start(startFuture: Future<Void>) {
        super.start(startFuture)
        timerId = vertx.setPeriodic(1000 * 60 * 20) { handleTick() }
    }

    override fun stop(stopFuture: Future<Void>?) {
        vertx.cancelTimer(timerId)
        super.stop(stopFuture)
    }

    fun handleTick() {
        with(table as WebSession) {
            spawn {
                db {
                    sqlStateAware {
                        log.trace("WebSessionVerticle::tick")
                        val now = System.currentTimeMillis()
                        deleteFrom(table).where((LAST_ACCESSED + TIMEOUT).gt(now))
                    }
                }
            }
        }
    }

    // For now, this is the only way to guarantee that read-after-write does not behave
    // differently from what vert.x-web expects it to do
    val mutex = Mutex()

    override suspend fun handleRead(message: WebSessionRecord): WebSessionRecord = mutex.withLock {
        super.handleRead(message)
    }

    override suspend fun handleUpdate(message: WebSessionRecord): WebSessionRecord = mutex.withLock {
        log.trace("Update requested in table ${table.name}:\n$message")
        handleCreateOrUpdate(message)
    }

    suspend override fun handleCreate(message: WebSessionRecord): WebSessionRecord = mutex.withLock {
        log.trace("Create requested in table ${table.name}:\n$message")
        handleCreateOrUpdate(message)
    }

    override suspend fun handleDelete(message: WebSessionRecord): WebSessionRecord = mutex.withLock {
        super.handleDelete(message)
    }

    suspend fun handleCreateOrUpdate(message: WebSessionRecord): WebSessionRecord {
        val table = WebSession.WEB_SESSION
        return db {
            sqlStateAware {
                withTransaction {
                    val prev = selectFrom(table)
                            .where(table.ID.eq(message.id))
                            .forUpdate()
                            .noWait()
                            .fetch()
                            .into(recordClass)
                            .firstOrNull()

                    log.trace("Previous record:\n$prev")

                    require(prev == null || prev.version == message.version) { "Conflict" }

                    ++message.version

                    insertInto(table)
                            .set(message)
                            .onConflict(WebSession.WEB_SESSION.ID)
                            .doUpdate()
                            .set(message)
                            .returning()
                            .fetch()
                            .into(recordClass)
                            .first()
                            .apply { log.trace("Inserted:\n$this") }
                }


            }
        }
    }

    suspend override fun handleBatchCreate(message: List<WebSessionRecord>): List<WebSessionRecord> =
            throw UnsupportedOperationException()
}
