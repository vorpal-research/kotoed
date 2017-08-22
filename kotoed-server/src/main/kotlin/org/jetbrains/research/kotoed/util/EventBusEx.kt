package org.jetbrains.research.kotoed.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.Warnings.DEPRECATION
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableRecord
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any): Message<ReturnType> =
        vxa { send(address, message, it) }

@JvmName("sendJsonAsync")
suspend fun EventBus.sendAsync(address: String, message: Any): Message<JsonObject> =
        sendAsync<JsonObject>(address, message)

@JvmName("trySendJsonAsync")
suspend fun EventBus.trySendAsync(address: String, message: Any): Message<JsonObject>? =
        try {
            sendAsync<JsonObject>(address, message)
        } catch (ex: ReplyException) {
            null
        }

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("sendAsync(address, message.toJson())"))
@Suppress("UNUSED_PARAMETER")
suspend fun EventBus.sendAsync(address: String, message: Jsonable): Unit = Unit

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("reply(message.toJson())"))
@Suppress("UNUSED_PARAMETER", "EXTENSION_SHADOWED_BY_MEMBER")
suspend fun <T> Message<T>.reply(message: Jsonable): Unit = Unit

fun EventBus.sendJsonable(address: String, message: Jsonable) =
        send(address, message.toJson())

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): Result {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync(address, toJson(value)).body().let(fromJson).uncheckedCast<Result>()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableCollectAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): List<Result> {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync<JsonArray>(address, toJson(value))
            .body()
            .asSequence()
            .filterIsInstance<JsonObject>()
            .map(fromJson)
            .map { it.uncheckedCast<Result>() }
            .toList()
}


inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > EventBus.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return sendJsonableAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > EventBus.sendJsonableCollectAsync(address: String, value: Argument): List<Result> {
    @Suppress(DEPRECATION)
    return sendJsonableCollectAsync(address, value, Argument::class, Result::class)
}

@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerFor(val address: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerFor(val address: String)


@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerForDynamic(val addressProperty: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerForDynamic(val addressProperty: String)

private fun getToJsonConverter(type: KType): (value: Any) -> Any {
    val klazz = type.jvmErasure
    return when {
        klazz == JsonObject::class -> {
            { it.expectingIs<JsonObject>() }
        }
        klazz.isSubclassOf(Jsonable::class) -> {
            { it.expectingIs<Jsonable>().toJson() }
        }
        klazz.isSubclassOf(Record::class) -> {
            { it.expectingIs<Record>().toJson() }
        }
        klazz == Unit::class -> {
            { JsonObject() }
        }

    // collections
        klazz == JsonArray::class -> {
            { it }
        }
        klazz.isSubclassOf(Collection::class) -> {
            val elementMapper = getToJsonConverter(type.arguments.first().type!!);
            {
                (it as Collection<*>)
                        .asSequence()
                        .filterNotNull()
                        .map(elementMapper)
                        .toList()
                        .let(::JsonArray)
            }
        }

        else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
    }
}

private fun getFromJsonConverter(type: KType): (value: Any) -> Any {
    val klazz = type.jvmErasure
    return when {
        klazz == JsonObject::class -> {
            { it.expectingIs<JsonObject>() }
        }
        klazz.isSubclassOf(Jsonable::class) -> {
            { fromJson(it.expectingIs<JsonObject>(), klazz) }
        }
        klazz.isSubclassOf(Record::class) -> {
            { it.expectingIs<JsonObject>().toRecord(klazz.uncheckedCast<KClass<out Record>>()) }
        }
        klazz == Unit::class -> {
            {}
        }

    // collections
        klazz == JsonArray::class -> {
            { it.expectingIs<JsonArray>() }
        }
        klazz.isSubclassOf(List::class) -> {
            val elementMapper = getFromJsonConverter(type.arguments.first().type!!);
            { it.expectingIs<JsonArray>().map(elementMapper) }
        }
        klazz.isSubclassOf(Set::class) -> {
            val elementMapper = getFromJsonConverter(type.arguments.first().type!!);
            { it.expectingIs<JsonArray>().map(elementMapper).toSet() }
        }

        else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
    }
}

object ConsumerAutoRegister : Loggable

fun AbstractKotoedVerticle.registerAllConsumers() {
    val klass = this::class

    for (function in klass.memberFunctions) {
        for (annotation in function.annotations) {
            when (annotation) {
                is EventBusConsumerFor ->
                    registerRawConsumer(function, annotation.address)
                is JsonableEventBusConsumerFor ->
                    registerJsonableConsumer(function, annotation.address)
                is EventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerRawConsumer(function, address)
                }
                is JsonableEventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerJsonableConsumer(function, address)
                }
            }
        }
    }
}

private fun AbstractVerticle.registerRawConsumer(
        function: KFunction<*>,
        address: String
) {
    val klass = this::class
    val eb = vertx.eventBus()
    ConsumerAutoRegister.log.info(
            "Auto-registering raw consumer for address $address \n" +
                    "using function $function"
    )

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(UnconfinedWithExceptions(msg)) {
                function.callAsync(this@registerRawConsumer, msg)
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            DelegateLoggable(klass.java).withExceptions(msg) {
                function.call(this@registerRawConsumer, msg)
            }
        }
    }
}

private fun AbstractVerticle.registerJsonableConsumer(
        function: KFunction<*>,
        address: String
) {
    val klass = this::class
    val eb = vertx.eventBus()

    ConsumerAutoRegister.log.info(
            "Auto-registering json-based consumer for address $address \n" +
                    "using function $function"
    )

    // first parameter is the receiver, we need the second one
    val parameterType = function.parameters[1].type
    val resultType = function.returnType
    val toJson = getToJsonConverter(resultType)
    val fromJson = getFromJsonConverter(parameterType)

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(UnconfinedWithExceptions(msg)) {
                val argument = fromJson(msg.body())
                val res = expectNotNull(function.callAsync(this@registerJsonableConsumer, argument))
                msg.reply(toJson(res))
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            DelegateLoggable(klass.java).withExceptions(msg) {
                val argument = fromJson(msg.body())
                val res = expectNotNull(function.call(this@registerJsonableConsumer, argument))
                msg.reply(toJson(res))
            }
        }
    }
}

object DebugInterceptor : Handler<SendContext<*>>, Loggable {
    override fun handle(event: SendContext<*>) {
        val message = event.message()
        log.trace("Message to ${message.address()}[${message.replyAddress() ?: ""}]")
        event.next()
    }
}

open class AbstractKotoedVerticle : AbstractVerticle() {
    override fun start(startFuture: Future<Void>) {
        registerAllConsumers()
        super.start(startFuture)
    }

    // all this debauchery is here due to a kotlin compiler bug:
    // https://youtrack.jetbrains.com/issue/KT-17640
    protected suspend fun <R : TableRecord<R>> dbUpdateAsync(v: R, klass: KClass<out R> = v::class): R =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(Address.DB.update(v.table.name), v, klass, klass)

    protected suspend fun <R : TableRecord<R>> dbCreateAsync(v: R, klass: KClass<out R> = v::class): R =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(Address.DB.create(v.table.name), v, klass, klass)

    protected suspend fun <R : TableRecord<R>> dbFetchAsync(v: R, klass: KClass<out R> = v::class): R =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(Address.DB.read(v.table.name), v, klass, klass)

    protected suspend fun <R : TableRecord<R>> dbFindAsync(v: R, klass: KClass<out R> = v::class): List<R> =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableCollectAsync(Address.DB.find(v.table.name), v, klass, klass)

    protected suspend fun dbQueryAsync(q: ComplexDatabaseQuery): List<JsonObject> =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableCollectAsync(Address.DB.query(q.table!!), q, ComplexDatabaseQuery::class, JsonObject::class)


    protected suspend fun <R : TableRecord<R>> dbProcessAsync(v: R, klass: KClass<out R> = v::class): VerificationData =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(Address.DB.process(v.table.name), v, klass, VerificationData::class)

    protected suspend fun <R : TableRecord<R>> dbVerifyAsync(v: R, klass: KClass<out R> = v::class): VerificationData =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(Address.DB.verify(v.table.name), v, klass, VerificationData::class)

    protected suspend fun <R : TableRecord<R>> fetchByIdAsync(instance: Table<R>, id: Int,
                                                              klass: KClass<out R> = instance.recordType.kotlin): R =
            @Suppress(DEPRECATION)
            vertx.eventBus().sendJsonableAsync(
                    Address.DB.read(instance.name),
                    JsonObject("id" to id),
                    JsonObject::class, klass)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotoedVerticle.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotoedVerticle.sendJsonableCollectAsync(address: String, value: Argument): List<Result> {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableCollectAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotoedVerticle.trySendJsonableAsync(address: String, value: Argument): Result? {
    return try {
        sendJsonableAsync(address, value)
    } catch (ex: ReplyException) {
        if (ex.failureType() == ReplyFailure.NO_HANDLERS) null
        else throw ex
    }
}
