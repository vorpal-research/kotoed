package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.PRNG
import io.vertx.ext.web.Session
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.sstore.impl.SessionImpl
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.WebSessionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.requestUUID
import org.jetbrains.research.kotoed.util.withRequestUUID

private typealias MessageRes = AsyncResult<Message<JsonObject>>

class AsyncSessionStore(val vertx: Vertx) : SessionStore, Loggable {
    private fun Session.asRecord(): WebSessionRecord {
        this as SessionImpl
        val buf = Buffer.buffer()
        writeToBuffer(buf)

        return WebSessionRecord().apply {
            id = id()
            lastAccessed = lastAccessed()
            timeout = timeout()
            data = java.util.Base64.getEncoder().encodeToString(buf.bytes)
        }
    }

    private fun WebSessionRecord.asSession(): Session {
        val buf = Buffer.buffer(java.util.Base64.getDecoder().decode(data))
        val session = SessionImpl(random)
        session.readFromBuffer(0, buf)
        return session
    }

    private fun Session.rep() = "Session{ " +
            "id = ${id()}, " +
            "lastAccessed = ${lastAccessed()}, " +
            "timeout = ${timeout()}, " +
            "data = ${data()} }"

    private val random: PRNG = PRNG(vertx)

    override fun createSession(timeout: Long): Session {
        return SessionImpl(random, timeout, SessionStore.DEFAULT_SESSIONID_LENGTH)
    }

    override fun createSession(timeout: Long, length: Int): Session {
        return SessionImpl(random, timeout, length)
    }

    override fun retryTimeout(): Long {
        return 0 // do not retry ever
    }

    override fun close() {}

    override fun clear(resultHandler: Handler<AsyncResult<Boolean>>) {
        // do not support clearing, it should clean itself
        // besides, there are no usages of this method anyways
        resultHandler.handle(Future.succeededFuture(true))
    }

    override fun put(session: Session, resultHandler: Handler<AsyncResult<Boolean>>) {
        val deliveryOptions = withRequestUUID()
        log.info("Assigning ${deliveryOptions.requestUUID()} to put(${session.id()})")

        session as SessionImpl
        get(session.id()) { getResult ->
            log.info("Getting: ${getResult.succeeded()}")
            if(getResult.failed()) {
                log.error("Getting existing session failed with ${getResult.cause()}")
                return@get resultHandler.handle(getResult.map(false))
            }

            val oldSession = getResult.result()
            log.info("Old session: $oldSession")
            when(oldSession) {
                null -> {
                    session.incrementVersion()
                    vertx.eventBus().send(
                            Address.DB.create(Tables.WEB_SESSION.name),
                            session.asRecord().toJson(),
                            deliveryOptions
                    ) { mes: AsyncResult<Message<JsonObject>> ->
                        resultHandler.handle(mes.map(false))
                    }
                }
                is SessionImpl -> {
                    if(session.version() != oldSession.version())
                        resultHandler.handle(Future.failedFuture("Version mismatch"))
                    else {
                        session.incrementVersion()
                        vertx.eventBus().send(
                                Address.DB.update(Tables.WEB_SESSION.name),
                                session.asRecord().toJson(),
                                deliveryOptions
                        ) { mes: AsyncResult<Message<JsonObject>> ->
                            resultHandler.handle(mes.map(true))
                        }
                    }
                }
                else -> resultHandler.handle(Future.failedFuture(IllegalStateException()))
            }
        }
    }

    override fun size(resultHandler: Handler<AsyncResult<Int>>) {
        val deliveryOptions = withRequestUUID()
        log.info("Assigning ${deliveryOptions.requestUUID()} to size request")
        vertx.eventBus().send(
                Address.DB.count(Tables.WEB_SESSION.name),
                ComplexDatabaseQuery(Tables.WEB_SESSION).toJson(),
                deliveryOptions
        ) { mes: MessageRes ->
            resultHandler.handle(mes.map { it.body().getInteger("count") })
        }
    }

    override fun get(id: String, resultHandler: Handler<AsyncResult<Session?>>) {
        val deliveryOptions = withRequestUUID()
        log.info("Assigning ${deliveryOptions.requestUUID()} to get($id)")

        vertx.eventBus().send(
                Address.DB.read(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                deliveryOptions
        ) { mes: MessageRes ->
            resultHandler.handle(mes
                    .map { it.body().toRecord<WebSessionRecord>().asSession() }
                    .otherwise(null as Session?)
                    .map { log.trace("get result: $it"); it }
            )
        }
    }


    override fun delete(id: String, resultHandler: Handler<AsyncResult<Boolean>>) {
        val deliveryOptions = withRequestUUID()
        log.info("Assigning ${deliveryOptions.requestUUID()} to delete($id)")

        vertx.eventBus().send(
                Address.DB.delete(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                deliveryOptions
        ) { mes: MessageRes ->
            // supposed to be otherwise(false), but LocalSessionStorage never returns false
            resultHandler.handle(mes.map { true }.otherwise(true))
        }
    }



}