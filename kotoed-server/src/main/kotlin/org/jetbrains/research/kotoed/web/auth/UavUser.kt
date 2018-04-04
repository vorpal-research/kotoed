package org.jetbrains.research.kotoed.web.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.impl.ClusterSerializable
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.data.db.HasPermMsg
import org.jetbrains.research.kotoed.data.db.HasPermReply
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

class UavUser() : User, Loggable, ClusterSerializable {
    lateinit var vertx: Vertx; private set
    lateinit var denizenId: String; private set
    var id: Int = -1; private set

    constructor(vertx: Vertx, denizenId: String, id: Int) : this() {
        this.vertx = vertx
        this.denizenId = denizenId
        this.id = id
    }

    override fun readFromBuffer(pos: Int, buffer: Buffer): Int {
        var ppos = pos
        val len = buffer.getInt(ppos)
        ppos += Integer.BYTES
        val bytes = buffer.getBytes(ppos, ppos + len)
        denizenId = bytes.toString(Charsets.UTF_8)
        ppos += len
        id = buffer.getInt(ppos)
        ppos += Integer.BYTES
        return ppos
    }

    override fun writeToBuffer(buffer: Buffer) {
        val bytes = denizenId.toByteArray(Charsets.UTF_8)
        buffer.appendInt(bytes.size)
        buffer.appendBytes(bytes)
        buffer.appendInt(id)
    }

    override fun isAuthorised(authority: String, handler: Handler<AsyncResult<Boolean>>): User = apply {
        val uuid = newRequestUUID()
        log.trace("Assigning $uuid to authority request for $authority")
        vertx.eventBus().send(
                Address.User.Auth.HasPerm,
                HasPermMsg(denizenId = denizenId, perm = authority).toJson(),
                withRequestUUID(uuid),
                Handler { ar: AsyncResult<Message<JsonObject>> ->
                    handler.handle(ar.map { fromJson<HasPermReply>(it.body()).result})
                })
    }

    override fun clearCache(): User = this  // Cache? What cache?

    override fun setAuthProvider(ap: AuthProvider) {
        when(ap) {
            is AsyncAuthProvider -> vertx = ap.vertx
            else -> throw IllegalStateException("Unknown auth provider: $ap")
        }
    }

    override fun principal(): JsonObject = JsonObject(
            "denizenId" to denizenId,
            "id" to id
    )
}
