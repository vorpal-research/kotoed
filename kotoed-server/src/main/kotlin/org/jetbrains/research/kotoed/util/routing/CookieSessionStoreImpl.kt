package org.jetbrains.research.kotoed.util.routing

import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.auth.VertxContextPRNG
import javax.crypto.spec.SecretKeySpec
import java.security.NoSuchAlgorithmException
import java.lang.RuntimeException
import org.jetbrains.research.kotoed.util.routing.CookieSessionStoreImpl.CookieSession
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.lang.Void
import io.vertx.ext.web.sstore.AbstractSession
import io.vertx.ext.auth.PRNG
import io.vertx.ext.web.Session
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl
import org.jetbrains.research.kotoed.util.executeBlockingAsync
import org.jetbrains.research.kotoed.util.localVertx
import java.lang.NullPointerException
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.util.*
import javax.crypto.Mac

/**
 * @author [Paulo Lopes](mailto:plopes@redhat.com)
 */
class CookieSessionStoreImpl : SessionStore {
    constructor() {
        // required for the service loader
    }

    constructor(vertx: Vertx, secret: String?) {
        init(vertx, JsonObject().put("secret", secret))
    }

    private lateinit var mac: Mac
    private lateinit var random: VertxContextPRNG
    private lateinit var oldStore: AsyncSessionStore

    override fun init(vertx: Vertx, options: JsonObject): SessionStore {
        // initialize a secure random
        random = VertxContextPRNG.current(vertx)
        mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(options.getString("secret").toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        oldStore = AsyncSessionStore(vertx)
        return this
    }

    override fun retryTimeout(): Long {
        return -1
    }

    override fun createSession(timeout: Long): Session {
        return CookieSession(mac, random, timeout, SessionStore.DEFAULT_SESSIONID_LENGTH)
    }

    override fun createSession(timeout: Long, length: Int): Session {
        return CookieSession(mac, random, timeout, length)
    }

    private fun doGet(session: CookieSession?, resultHandler: Handler<AsyncResult<Session?>>) {
        try {

            if (session == null) {
                resultHandler.handle(Future.succeededFuture())
                return
            }

            // need to validate for expired
            val now = System.currentTimeMillis()
            // if expired, the operation succeeded, but returns null
            if (now - session.lastAccessed() > session.timeout()) {
                resultHandler.handle(Future.succeededFuture())
            } else {
                // return the already recreated session
                resultHandler.handle(Future.succeededFuture(session))
            }
        } catch (e: Exception) {
            resultHandler.handle(Future.failedFuture(e))
        }
    }

    override fun get(cookieValue: String, resultHandler: Handler<AsyncResult<Session?>>) {
        try {
            if (runCatching { UUID.fromString(cookieValue) }.isSuccess) {
                oldStore.get(cookieValue) { res ->
                    if (res.failed()) {
                        resultHandler.handle(res)
                    }
                    val cs = (res.result() as? AbstractSession)?.let { CookieSession(mac, random, it) }
                    doGet(cs, resultHandler)
                }
            } else {
                val session: CookieSession? = CookieSession(mac, random).setValue(cookieValue)
                doGet(session, resultHandler)
            }
        } catch (e: RuntimeException) {
            resultHandler.handle(Future.failedFuture(e))
        }
    }

    override fun delete(id: String, resultHandler: Handler<AsyncResult<Void>>) {
        resultHandler.handle(Future.succeededFuture())
    }

    override fun put(session: Session, resultHandler: Handler<AsyncResult<Void>>) {
        val cookieSession = session as CookieSession
        if (cookieSession.oldVersion() != -1) {
            // there was already some stored data in this case we need to validate versions
            if (cookieSession.oldVersion() != cookieSession.version()) {
                resultHandler.handle(Future.failedFuture("Version mismatch"))
                return
            }
        }
        cookieSession.incrementVersion()
        resultHandler.handle(Future.succeededFuture())
    }

    override fun clear(resultHandler: Handler<AsyncResult<Void>>) {
        resultHandler.handle(Future.succeededFuture())
    }

    override fun size(resultHandler: Handler<AsyncResult<Int>>) {
        resultHandler.handle(Future.succeededFuture(0))
    }

    override fun close() {
        // nothing to close
    }

    internal class CookieSession : SharedDataSessionImpl {
        private val mac: Mac?

        // track the original version
        private var oldVersion = 0

        // track the original crc
        private var oldCrc = 0

        constructor(mac: Mac?, prng: VertxContextPRNG?, timeout: Long, length: Int) : super(
            prng as PRNG?,
            timeout,
            length
        ) {
            this.mac = mac
        }

        constructor(mac: Mac?, prng: VertxContextPRNG?) : super(prng as PRNG?) {
            this.mac = mac
        }

        constructor(mac: Mac?, prng: VertxContextPRNG?, otherSession: AbstractSession): super(prng as PRNG?) {
            this.mac = mac

            this.setId(otherSession.id())
            this.setVersion(otherSession.version())
            this.setLastAccessed(otherSession.lastAccessed())
            this.setTimeout(otherSession.timeout())
            this.setData(otherSession.data())
        }

        override fun value(): String {
            val payload = Buffer.buffer()
            writeToBuffer(payload)
            val b64 = ENCODER.encodeToString(payload.bytes)
            val signature = ENCODER.encodeToString(mac!!.doFinal(b64.toByteArray(StandardCharsets.US_ASCII)))
            return "$b64.$signature"
        }

        override fun isRegenerated(): Boolean {
            return if (!super.isRegenerated()) {
                // force a new checksum calculation
                oldCrc != checksum()
            } else true
        }

        fun setValue(payload: String?): CookieSession? {
            if (payload == null) {
                throw NullPointerException()
            }
            val tokens = payload.split("\\.".toRegex()).toTypedArray()
            if (tokens.size != 2) {
                // no signature present, force a regeneration
                // by claiming this session as invalid
                return null
            }
            val signature = ENCODER.encodeToString(mac!!.doFinal(tokens[0].toByteArray(StandardCharsets.US_ASCII)))
            if (signature != tokens[1]) {
                throw RuntimeException("Session data was Tampered!")
            }

            // reconstruct the session
            readFromBuffer(0, Buffer.buffer(DECODER.decode(tokens[0])))
            // defaults
            oldVersion = version()
            oldCrc = crc()
            return this
        }

        fun oldVersion(): Int {
            return oldVersion
        }

        companion object {
            private val ENCODER = Base64.getUrlEncoder().withoutPadding()
            private val DECODER = Base64.getUrlDecoder()
        }
    }
}