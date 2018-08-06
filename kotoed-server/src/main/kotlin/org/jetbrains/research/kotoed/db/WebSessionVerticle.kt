package org.jetbrains.research.kotoed.db

import io.vertx.core.Future
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

    suspend override fun handleCreate(message: WebSessionRecord): WebSessionRecord {
        log.trace("Create requested in table ${table.name}:\n$message")
        // this is the same as regular create, but without resetting primary keys
        return db {
            sqlStateAware {
                insertInto(table)
                        .set(message)
                        .returning()
                        .fetch()
                        .into(recordClass)
                        .first()
            }
        }
    }

    suspend override fun handleBatchCreate(message: List<WebSessionRecord>): List<WebSessionRecord> =
            throw UnsupportedOperationException()
}