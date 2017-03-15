package org.jetbrains.research.kotoed.util.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import org.jetbrains.research.kotoed.util.vx

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any): Message<ReturnType> =
        vx<Message<ReturnType>> { send(address, message, it) }
