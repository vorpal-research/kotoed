package org.jetbrains.research.kotoed.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

import kotlinx.coroutines.experimental.launch
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any): Message<ReturnType> =
        vxa { send(address, message, it) }

@JvmName("sendJsonAsync")
suspend fun EventBus.sendAsync(address: String, message: Any): Message<JsonObject> =
        sendAsync<JsonObject>(address, message)

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("sendAsync(address, message.toJson())"))
@Suppress("UNUSED_PARAMETER")
suspend fun EventBus.sendAsync(address: String, message: Jsonable): Unit = Unit

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("reply(message.toJson())"))
@Suppress("UNUSED_PARAMETER")
suspend fun <T> Message<T>.reply(message: Jsonable): Unit = Unit

fun EventBus.sendJsonable(address: String, message: Jsonable) =
        send(address, message.toJson())

@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerFor(val address: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerFor(val address: String)

fun AbstractVerticle.registerAllConsumers() {
    val klass = this::class

    val eb = vertx.eventBus()

    for (function in klass.declaredMemberFunctions) {
        for (annotation in function.annotations)
            when (annotation) {
                is EventBusConsumerFor ->
                    if (function.isSuspend) {
                        eb.consumer<JsonObject>(annotation.address)
                        { msg ->
                            launch(UnconfinedWithExceptions(msg)) {
                                function.callAsync(this@registerAllConsumers, msg)
                            }
                        }
                    } else {
                        eb.consumer<JsonObject>(annotation.address)
                        { msg -> function.call(this, msg) }
                    }
                is JsonableEventBusConsumerFor ->
                    if (function.isSuspend) {
                        // first parameter is the receiver, we need the second one
                        val parameterClass = function.parameters[1].type.jvmErasure
                        assert(parameterClass.isSubclassOf(Jsonable::class))

                        eb.consumer<JsonObject>(annotation.address)
                        { msg ->
                            launch(UnconfinedWithExceptions(msg)) {
                                val argument = fromJson(msg.body(), parameterClass)
                                val res = function.callAsync(this@registerAllConsumers, argument) as Jsonable
                                msg.reply(res.toJson())
                            }
                        }
                    } else {
                        // first parameter is the receiver, we need the second one
                        val parameterClass = function.parameters[1].type.jvmErasure
                        assert(parameterClass.isSubclassOf(Jsonable::class))

                        eb.consumer<JsonObject>(annotation.address)
                        { msg ->
                            val argument = fromJson(msg.body(), parameterClass)
                            val res = function.call(this@registerAllConsumers, argument) as Jsonable
                            msg.reply(res.toJson())
                        }
                    }
            }
    }
}

open class AbstractKotoedVerticle: AbstractVerticle() {
    override fun start() { registerAllConsumers() }
}
