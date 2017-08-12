package org.jetbrains.research.kotoed.auxiliary.data

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.database.toJson
import org.jooq.Record
import java.time.LocalDateTime

data class TimetableMessage(
        val message: JsonObject,
        val time: LocalDateTime,
        val sendTo: String,
        val replyTo: String? = null
): Jsonable {
    constructor(message: Jsonable, time: LocalDateTime, sendTo: String, replyTo: String?) :
            this(message.toJson(), time, sendTo, replyTo)

    constructor(message: Record, time: LocalDateTime, sendTo: String, replyTo: String?) :
            this(message.toJson(), time, sendTo, replyTo)
}
