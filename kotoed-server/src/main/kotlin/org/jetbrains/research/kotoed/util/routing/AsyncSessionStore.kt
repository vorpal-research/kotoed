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
import org.jetbrains.research.kotoed.util.withRequestUUID

private typealias MessageRes = AsyncResult<Message<JsonObject>>

class AsyncSessionStore(val vertx: Vertx) : SessionStore, Loggable {
    private fun Session.asRecord(): WebSessionRecord {
        log.trace("Serializing: " + this.rep())
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
        log.trace("Deserialized: " + session.rep())
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
        return 500
    }

    override fun close() {}

    override fun clear(resultHandler: Handler<AsyncResult<Boolean>>) {
        // do not support clearing, it should clean itself
        // besides, there are no usages of this method anyways
        resultHandler.handle(Future.succeededFuture(true))
    }

    override fun put(session: Session, resultHandler: Handler<AsyncResult<Boolean>>) {
        log.trace("put $session")

        session as SessionImpl
        get(session.id()) { getResult ->
            if(getResult.failed())
                return@get resultHandler.handle(getResult.map(false))

            val oldSession = getResult.result()
            when(oldSession) {
                null -> {
                    session.incrementVersion()
                    vertx.eventBus().send(
                            Address.DB.create(Tables.WEB_SESSION.name),
                            session.asRecord().toJson(),
                            withRequestUUID()
                    ) { mes: AsyncResult<Message<JsonObject>> ->
                        resultHandler.handle(mes.map(false))
                    }
                }
                is SessionImpl -> {
                    if(session.version() != oldSession.version())
                        resultHandler.handle(Future.failedFuture("Version mismatch"))
                    session.incrementVersion()
                    vertx.eventBus().send(
                            Address.DB.update(Tables.WEB_SESSION.name),
                            session.asRecord().toJson(),
                            withRequestUUID()
                    ) { mes: AsyncResult<Message<JsonObject>> ->
                        resultHandler.handle(mes.map(true))
                    }
                }
                else -> resultHandler.handle(Future.failedFuture(IllegalStateException()))
            }
        }
    }

    override fun size(resultHandler: Handler<AsyncResult<Int>>) {
        log.trace("size")

        vertx.eventBus().send(
                Address.DB.count(Tables.WEB_SESSION.name),
                ComplexDatabaseQuery(Tables.WEB_SESSION).toJson(),
                withRequestUUID()
        ) { mes: MessageRes ->
            resultHandler.handle(mes.map { it.body().getInteger("count") })
        }
    }

    override fun get(id: String, resultHandler: Handler<AsyncResult<Session?>>) {
        log.trace("get $id")

        vertx.eventBus().send(
                Address.DB.read(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                withRequestUUID()
        ) { mes: MessageRes ->
            resultHandler.handle(mes
                    .map { it.body().toRecord<WebSessionRecord>().asSession() }
                    .otherwise(null as Session?)
                    .map { log.trace("get result: $it"); it }
            )
        }
    }


    override fun delete(id: String, resultHandler: Handler<AsyncResult<Boolean>>) {
        log.trace("delete $id")
        vertx.eventBus().send(
                Address.DB.delete(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                withRequestUUID()
        ) { mes: MessageRes ->
            resultHandler.handle(mes.map { true }.otherwise(false))
        }
    }



}