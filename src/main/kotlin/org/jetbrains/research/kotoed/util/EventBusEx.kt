package org.jetbrains.research.kotoed.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlin.reflect.full.declaredMemberFunctions

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any): Message<ReturnType> =
        vxa { send(address, message, it) }

@JvmName("sendJsonAsync")
suspend fun EventBus.sendAsync(address: String, message: Any): Message<JsonObject> =
        sendAsync<JsonObject>(address, message)

@Deprecated("Forgot to call .toJson()?", level = DeprecationLevel.ERROR)
@Suppress("UNUSED_PARAMETER")
suspend fun EventBus.sendAsync(address: String, message: Jsonable): Unit = Unit

fun EventBus.sendJsonable(address: String, message: Jsonable) =
        send(address, message.toJson())

@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerFor(val address: String)

fun AbstractVerticle.registerAllConsumers() {
    val klass = this::class

    val eb = vertx.eventBus()

    for (function in klass.declaredMemberFunctions) {
        function.annotations
                .find { it.annotationClass == EventBusConsumerFor::class }
                ?.also {
                    eb.consumer<JsonObject>(
                            (it as EventBusConsumerFor).address,
                            { msg -> function.call(this, msg) }
                    )
                }
    }
}
