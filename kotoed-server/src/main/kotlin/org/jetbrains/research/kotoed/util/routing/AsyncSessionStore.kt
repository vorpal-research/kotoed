package org.jetbrains.research.kotoed.util.routing

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
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
import org.jetbrains.research.kotoed.util.GlobalLogging.log
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.util.requestUUID
import org.jetbrains.research.kotoed.util.withRequestUUID

private typealias MessageRes = AsyncResult<Message<JsonObject>>

class MySessionImpl : SessionImpl {
    constructor(store: AsyncSessionStore) :
            super()
    constructor(store: AsyncSessionStore, random: PRNG?) :
            super(random)
    constructor(store: AsyncSessionStore, random: PRNG?, timeout: Long, length: Int) :
            super(random, timeout, length)

    override fun regenerateId(): Session {
        super.regenerateId()
        log.trace("regenerateId: oldId = ${oldId()}, newId = ${id()}")
        return this
    }

    override fun toString(): String = "Session{ " +
            "id = ${id()}, " +
            "lastAccessed = ${lastAccessed()}, " +
            "timeout = ${timeout()}, " +
            "version = ${(this as SessionImpl).version()}, " +
            "data = ${data()} }"
}

class AsyncSessionStore(val vertx: Vertx) : SessionStore, Loggable {
    private fun Session.asRecord(): WebSessionRecord {
        this as SessionImpl
        val buf = Buffer.buffer()
        writeToBuffer(buf)
        return WebSessionRecord().apply {
            id = id()
            lastAccessed = lastAccessed()
            timeout = timeout()
            version = version()
            data = java.util.Base64.getEncoder().encodeToString(buf.bytes)
        }
    }

    private fun WebSessionRecord.asSession(): Session {
        val buf = Buffer.buffer(java.util.Base64.getDecoder().decode(data))
        val session = MySessionImpl(this@AsyncSessionStore, random)
        session.readFromBuffer(0, buf)
        // yes, this is a bit stupid
        while(session.version() != version) session.incrementVersion()

        return session
    }
    private fun JsonObject.asSession(): Session = toRecord<WebSessionRecord>().asSession()

    private fun Session.rep() = "Session{ " +
            "id = ${id()}, " +
            "lastAccessed = ${lastAccessed()}, " +
            "timeout = ${timeout()}, " +
            "version = ${(this as SessionImpl).version()}, " +
            "data = ${data()} }"

    private val random: PRNG = PRNG(vertx)

    override fun createSession(timeout: Long): Session {
        return MySessionImpl(this, random, timeout, SessionStore.DEFAULT_SESSIONID_LENGTH)
    }

    override fun createSession(timeout: Long, length: Int): Session {
        return MySessionImpl(this, random, timeout, length)
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
        session as SessionImpl
        log.info("Assigning ${deliveryOptions.requestUUID()} to put(${session.rep()})")

        log.info("Writing session")
        vertx.eventBus().send(
                Address.DB.update(Tables.WEB_SESSION.name),
                session.asRecord().toJson(),
                deliveryOptions
        ) { mes: AsyncResult<Message<JsonObject>> ->
            if(mes.succeeded()) log.info("Returned: ${mes.result().body().asSession().rep()}")

            val oldId = session.oldId()
            if(oldId != null) {
                doDelete(oldId) {
                    resultHandler.handle(mes.map(false))
                }
            } else resultHandler.handle(mes.map(false))
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

        innerGet(id, deliveryOptions) { res ->
            resultHandler.handle(res)
        }
    }

    fun innerGet(id: String, deliveryOptions: DeliveryOptions,
                 resultHandler: (AsyncResult<Session?>) -> Unit) {
        vertx.eventBus().send(
                Address.DB.read(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                deliveryOptions
        ) { mes: MessageRes ->
            resultHandler(mes
                    .map { it.body().toRecord<WebSessionRecord>().asSession() }
                    .otherwise(null as Session?)
                    .map { log.trace("Get result: ${it?.rep()}"); it }
            )
        }
    }

    fun doDelete(id: String, resultHandler: (AsyncResult<Boolean>) -> Unit) {
        val deliveryOptions = withRequestUUID()
        log.info("Assigning ${deliveryOptions.requestUUID()} to delete($id)")

        vertx.eventBus().send(
                Address.DB.delete(Tables.WEB_SESSION.name),
                WebSessionRecord().apply { this.id = id }.toJson(),
                deliveryOptions
        ) { mes: MessageRes ->
            // supposed to be otherwise(false), but LocalSessionStorage never returns false
            resultHandler(mes.map { true }.otherwise(true))
        }
    }

    override fun delete(id: String, resultHandler: Handler<AsyncResult<Boolean>>) {
        doDelete(id, resultHandler::handle)
    }
}

class EasyAsyncSessionStore(val vertx: Vertx, val delegate: SessionStore = AsyncSessionStore(vertx)) :
        SessionStore by delegate, Loggable {

    val storage: MutableMap<String, SessionImpl> = mutableMapOf()
    override fun get(id: String, resultHandler: Handler<AsyncResult<Session>>) {
        if(id in storage) resultHandler.handle(Future.succeededFuture(storage[id]))
        else delegate.get(id, resultHandler)
    }

    override fun put(session: Session, resultHandler: Handler<AsyncResult<Boolean>>) {
        storage[session.id()] = session as SessionImpl
        delegate.put(session, resultHandler)
    }

    override fun delete(id: String, resultHandler: Handler<AsyncResult<Boolean>>) {
        storage.remove(id)
        delegate.delete(id, resultHandler)
    }
}
