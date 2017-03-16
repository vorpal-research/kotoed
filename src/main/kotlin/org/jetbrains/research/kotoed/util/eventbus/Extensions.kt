package org.jetbrains.research.kotoed.util.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import org.jetbrains.research.kotoed.util.vxa

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any): Message<ReturnType> =
        vxa { send(address, message, it) }
