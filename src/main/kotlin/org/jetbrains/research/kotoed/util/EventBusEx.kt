@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.research.kotoed.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Record
import org.jooq.Table
import org.jooq.UpdatableRecord
import kotlin.reflect.KClass
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

private fun getToJsonConverter(klazz: KClass<*>): (value: Any) -> JsonObject =
        when {
            klazz.isSubclassOf(JsonObject::class) -> {
                { it as JsonObject }
            }
            klazz.isSubclassOf(Jsonable::class) -> {
                { (it as Jsonable).toJson() }
            }
            klazz.isSubclassOf(Record::class) -> {
                { (it as Record).toJson() }
            }
            klazz.isSubclassOf(Unit::class) -> {
                { JsonObject() }
            }
            else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
        }

private fun getFromJsonConverter(klazz: KClass<*>): (value: JsonObject) -> Any =
        when {
            klazz.isSubclassOf(JsonObject::class) -> {
                { it }
            }
            klazz.isSubclassOf(Jsonable::class) -> {
                { fromJson(it, klazz) }
            }
            klazz.isSubclassOf(Record::class) -> {
                { it.toRecord(klazz as KClass<out Record>) }
            }
            klazz.isSubclassOf(Unit::class) -> {
                {}
            }
            else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
        }

fun AbstractVerticle.registerAllConsumers() {
    val klass = this::class

    val eb = vertx.eventBus()

    for (function in klass.declaredMemberFunctions) {
        for (annotation in function.annotations)
            when (annotation) {
                is EventBusConsumerFor ->
                    if (function.isSuspend) {
                        eb.consumer<JsonObject>(annotation.address) { msg ->
                            launch(UnconfinedWithExceptions(msg)) {
                                function.callAsync(this@registerAllConsumers, msg)
                            }
                        }
                    } else {
                        eb.consumer<JsonObject>(annotation.address) { msg ->
                            function.call(this, msg)
                        }
                    }
                is JsonableEventBusConsumerFor ->
                    if (function.isSuspend) {
                        // first parameter is the receiver, we need the second one
                        val parameterClass = function.parameters[1].type.jvmErasure
                        val resultClass = function.returnType.jvmErasure
                        val toJson = getToJsonConverter(resultClass)
                        val fromJson = getFromJsonConverter(parameterClass)

                        eb.consumer<JsonObject>(annotation.address) { msg ->
                            launch(UnconfinedWithExceptions(msg)) {
                                val argument = fromJson(msg.body())
                                val res = function.callAsync(this@registerAllConsumers, argument)!!
                                msg.reply(toJson(res))
                            }
                        }
                    } else {
                        // first parameter is the receiver, we need the second one
                        val parameterClass = function.parameters[1].type.jvmErasure
                        val resultClass = function.returnType.jvmErasure
                        val toJson = getToJsonConverter(resultClass)
                        val fromJson = getFromJsonConverter(parameterClass)

                        eb.consumer<JsonObject>(annotation.address) { msg ->
                            val argument = fromJson(msg.body())
                            val res = function.call(this@registerAllConsumers, argument)!!
                            msg.reply(toJson(res))
                        }
                    }
            }
    }
}

open class AbstractKotoedVerticle : AbstractVerticle() {
    override fun start(startFuture: Future<Void>) {
        registerAllConsumers()
        super.start(startFuture)
    }

    @PublishedApi
    @Deprecated("Do not call directly")
    internal suspend fun <Argument : Any, Result : Any> sendJsonableAsync(
            address: String,
            value: Argument,
            argClass: KClass<Argument>,
            resultClass: KClass<Result>
    ): Result {
        val toJson = getToJsonConverter(argClass)
        val fromJson = getFromJsonConverter(resultClass)
        return vertx.eventBus().sendAsync(address, toJson(value)).body().let(fromJson) as Result
    }

    // all this debauchery is here due to a kotlin compiler bug:
    // https://youtrack.jetbrains.com/issue/KT-17640
    protected suspend fun <R : UpdatableRecord<R>> persist(v: R, klass: KClass<R>): R =
            sendJsonableAsync(Address.DB.update(v.table.name), v, klass, klass)

    protected suspend fun <R : UpdatableRecord<R>> persistAsCopy(v: R, klass: KClass<R>): R =
            sendJsonableAsync(Address.DB.create(v.table.name), v, klass, klass)

    protected suspend fun <R : UpdatableRecord<R>> selectById(instance: Table<R>, id: Int, klass: KClass<R>): R =
            sendJsonableAsync(Address.DB.read(instance.name), JsonObject("id" to id), JsonObject::class, klass)

}

inline suspend fun <
        reified Argument : Any,
        reified Result : Any
        > AbstractKotoedVerticle.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress("DEPRECATION")
    return sendJsonableAsync(address, value, Argument::class, Result::class)
}

