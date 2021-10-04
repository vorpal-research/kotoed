package org.jetbrains.research.kotoed.util

import io.vertx.core.*
import io.vertx.core.eventbus.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.warnings.Warnings.DEPRECATION
import org.jetbrains.research.kotoed.data.api.CountResponse
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.db.BatchUpdateMsg
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.TypedQueryBuilder
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.database.fixTitle
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableRecord
import org.kohsuke.randname.RandomNameGenerator
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

/******************************************************************************/

const val KOTOED_REQUEST_UUID = "\$kotoedrequestuuid\$"

val REQUEST_UUID_GEN = RandomNameGenerator()

fun newRequestUUID(): String = REQUEST_UUID_GEN.next()

fun <T> Message<T>.requestUUID() = headers()[KOTOED_REQUEST_UUID] ?: "UNKNOWN"

fun withRequestUUID(uuid: String = newRequestUUID()): DeliveryOptions =
        DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, uuid)

fun DeliveryOptions.requestUUID() = headers[KOTOED_REQUEST_UUID] ?: "UNKNOWN"

/******************************************************************************/

// TODO(rename to requestAsync)
suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<ReturnType> {
    if (true != deliveryOptions.headers?.contains(KOTOED_REQUEST_UUID)) {
        deliveryOptions.addHeader(KOTOED_REQUEST_UUID,
                currentCoroutineName().name)
    }
    return vxa { request(address, message, deliveryOptions, it) }
}

@JvmName("sendJsonAsync")
suspend fun EventBus.sendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<JsonObject> =
        sendAsync<JsonObject>(address, message, deliveryOptions)

@JvmName("trySendJsonAsync")
suspend fun EventBus.trySendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<JsonObject>? =
        try {
            sendAsync<JsonObject>(address, message, deliveryOptions)
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

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any> EventBus.sendJsonable(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>
) {

    val toJson = getToJsonConverter(argClass.starProjectedType)
    val currentName = currentCoroutineName()
    send(address, toJson(value), DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, currentName.name))
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any> EventBus.publishJsonable(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>
) {

    val toJson = getToJsonConverter(argClass.starProjectedType)
    val currentName = currentCoroutineName()
    publish(address, toJson(value), DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, currentName.name))
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument> = value::class,
        resultClass: KClass<out Result>
): Result {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync(address, toJson(value)).body().let(fromJson).uncheckedCast<Result>()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableAsync(
        address: String,
        value: Argument,
        argType: KType? = null,
        resultType: KType
): Result {
    val toJson = getToJsonConverter(argType ?: value::class.starProjectedType)
    val fromJson = getFromJsonConverter(resultType)
    return sendAsync(address, toJson(value)).body().let(fromJson).uncheckedCast<Result>()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableCollectAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>,
        deliveryOptions: DeliveryOptions = DeliveryOptions()
): List<Result> {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync<JsonArray>(address, toJson(value), deliveryOptions = deliveryOptions)
            .body()
            .asSequence()
            .filterIsInstance<JsonObject>()
            .map(fromJson)
            .map { it.uncheckedCast<Result>() }
            .toList()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableCollectAsync(
        address: String,
        value: List<Argument>,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): List<Result> {
    val toJson = getToJsonConverter(
            List::class.createType(
                    listOf(KTypeProjection.invariant(argClass.starProjectedType))))
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
        reified Argument : Any
        > EventBus.sendJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return sendJsonable(address, value, Argument::class)
}

inline suspend fun <
        reified Argument : Any
        > EventBus.publishJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return publishJsonable(address, value, Argument::class)
}

@OptIn(ExperimentalStdlibApi::class)
inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > EventBus.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return sendJsonableAsync(address, value, typeOf<Argument>(), typeOf<Result>())
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
annotation class JsonableEventBusConsumerFor(val address: String, val nonCopy: Boolean = false)


@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerForDynamic(val addressProperty: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerForDynamic(val addressProperty: String, val nonCopy: Boolean = false)


@Target(AnnotationTarget.CLASS)
annotation class CleanupJsonFields(val fields: Array<String>)


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
            { fromJson(it.expectingIs<JsonObject>(), type) }
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

    val cleanupJsonFields = klass.annotations
            .filterIsInstance<CleanupJsonFields>()
            .firstOrNull()
            ?.fields ?: emptyArray()

    for (function in klass.memberFunctions) {
        for (annotation in function.annotations) {
            when (annotation) {
                is EventBusConsumerFor ->
                    registerRawConsumer(function, annotation.address, cleanupJsonFields)
                is JsonableEventBusConsumerFor ->
                    registerJsonableConsumer(function, annotation.address, annotation.nonCopy, cleanupJsonFields)
                is EventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerRawConsumer(function, address, cleanupJsonFields)
                }
                is JsonableEventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerJsonableConsumer(function, address, annotation.nonCopy, cleanupJsonFields)
                }
            }
        }
    }
}

private fun CoroutineVerticle.registerRawConsumer(
        function: KFunction<*>,
        address: String,
        cleanupJsonFields: Array<String>
) {
    val klass = this::class
    val eb = vertx.eventBus()
    ConsumerAutoRegister.log.info(
            "Auto-registering raw consumer for address $address \n" +
                    "using function $function"
    )

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(DelegateLoggable(klass.java).WithExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields))
                    + CoroutineName(msg.requestUUID())) {
                function.callAsync(this@registerRawConsumer, msg)
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            val oldName = Thread.currentThread().name
            try {
                Thread.currentThread().name = msg.requestUUID()
                DelegateLoggable(klass.java).withExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields)) {
                    function.call(this@registerRawConsumer, msg)
                }
            } finally {
                Thread.currentThread().name = oldName
            }
        }
    }
}

private fun CoroutineVerticle.registerJsonableConsumer(
        function: KFunction<*>,
        address: String,
        isNonCopy: Boolean,
        cleanupJsonFields: Array<String>
) {
    val klass = this::class
    val eb = vertx.eventBus()

    ConsumerAutoRegister.log.info(
            "Auto-registering json-based consumer for address $address \n" +
                    "using function $function"
    )

    // first parameter is the receiver, we need the second one
    val parameterType = function.parameters.getOrNull(1)?.type
    val resultType = function.returnType
    val toJson = getToJsonConverter(resultType)
    val fromJson = getFromJsonConverter(parameterType ?: (Unit::class).starProjectedType)

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(DelegateLoggable(klass.java).WithExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields))
                    + CoroutineName(msg.requestUUID())) {
                val argument = fromJson(msg.body())
                val res = when (parameterType) {
                    null -> expectNotNull(function.callAsync(this@registerJsonableConsumer))
                    else -> expectNotNull(function.callAsync(this@registerJsonableConsumer, argument))
                }
                val delOps = DeliveryOptions()
                if(isNonCopy) {
                    delOps.codecName = when {
                        resultType.jvmErasure.isSubclassOf(JsonObject::class) -> NonCopyJsonObjectCodec.name()
                        resultType.jvmErasure.isSubclassOf(JsonArray::class) -> NonCopyJsonArrayCodec.name()
                        else -> null
                    }
                }
                msg.reply(toJson(res), delOps)
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            val oldName = Thread.currentThread().name
            try {
                Thread.currentThread().name = msg.requestUUID()
                DelegateLoggable(klass.java).withExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields)) {
                    val argument = fromJson(msg.body())
                    val res = when (parameterType) {
                        null -> expectNotNull(function.call(this@registerJsonableConsumer))
                        else -> expectNotNull(function.call(this@registerJsonableConsumer, argument))
                    }
                    val delOps = DeliveryOptions()
                    if(isNonCopy) {
                        delOps.codecName = when {
                            resultType.jvmErasure.isSubclassOf(JsonObject::class) -> NonCopyJsonObjectCodec.name()
                            resultType.jvmErasure.isSubclassOf(JsonArray::class) -> NonCopyJsonArrayCodec.name()
                            else -> null
                        }
                    }

                    msg.reply(toJson(res), delOps)
                }
            } finally {
                Thread.currentThread().name = oldName
            }
        }
    }
}

object DebugInterceptor : Handler<DeliveryContext<Any?>>, Loggable {
    override fun handle(event: DeliveryContext<Any?>) {
        val message = event.message()
        log.trace("Message to ${message.address()}[${message.replyAddress() ?: ""}]")
        event.next()
    }
}

interface WithVertx {
    val vertxInstance: Vertx
}

val WithVertx.vertx: Vertx
    get() = vertxInstance

inline fun WithVertx(crossinline body: () -> Vertx) = object: WithVertx {
    override val vertxInstance: Vertx
        get() = body()
}

inline fun <R> withVertx(vertx: Vertx, body: WithVertx.() -> R): R = WithVertx { vertx }.body()
inline fun <R> withVertx(routing: RoutingContext, body: WithVertx.() -> R): R =
    withVertx(routing.vertx(), body)
inline fun <R> withVertx(verticle: Verticle, body: WithVertx.() -> R): R =
    withVertx(verticle.vertx, body)

open class VertxScope(override val vertxInstance: Vertx):
        Vertx by vertxInstance,
        WithVertx,
        CoroutineScope {
    override val coroutineContext: CoroutineContext by lazy { vertxInstance.dispatcher() }
}

val Vertx.scope
    get() = VertxScope(this)

open class AbstractKotoedVerticle : CoroutineVerticle(), CoroutineScope, Loggable, WithVertx {
    override suspend fun start() {
        registerAllConsumers()
        super.start()
    }

    override val vertxInstance: Vertx
        get() = super.getVertx()

}

suspend fun <V, R> V.async(dispatcher: CoroutineContext = this.coroutineContext, body: suspend () -> R)
where V: CoroutineScope, V: WithVertx, V: Loggable =
    (this as CoroutineScope).async(dispatcher + LogExceptions() + currentCoroutineName()) {
        body()
    }

fun <V, R> V.spawn(dispatcher: CoroutineContext = coroutineContext, body: suspend () -> R)
        where V: CoroutineScope, V: WithVertx, V: Loggable {
    var context = LogExceptions() + dispatcher
    if(context[CoroutineName.Key] == null) {
        val name = newRequestUUID()
        log.trace("Assigning $name to spawned call of $body")
        context += CoroutineName(name)
    }

    launch(context) { body() }
}

suspend fun <R : TableRecord<R>> WithVertx.dbUpdateAsync(v: R, klass: KClass<out R> = v::class): R =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.update(v.table.name), v, klass, klass)

suspend fun <R : TableRecord<R>> WithVertx.dbBatchUpdateAsync(criteria: R,
                                                              patch: R,
                                                              klass: KClass<out R> = criteria::class): Unit =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(
        Address.DB.batchUpdate(criteria.table.name),
        BatchUpdateMsg(criteria, patch), BatchUpdateMsg::class, Unit::class).also {
        use(klass)
    }

suspend fun <R : TableRecord<R>> WithVertx.dbCreateAsync(v: R, klass: KClass<out R> = v::class): R =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.create(v.table.name), v, klass, klass)

suspend fun <R : TableRecord<R>> WithVertx.dbBatchCreateAsync(v: List<R>): List<R> =
    @Suppress(DEPRECATION)
    if (v.isEmpty()) emptyList()
    else {
        val evidence = v.first()
        val klass = evidence::class
        vertx.eventBus().sendJsonableCollectAsync(Address.DB.batchCreate(evidence.table.name), v, klass, klass)
    }

suspend fun <R : TableRecord<R>> WithVertx.dbDeleteAsync(v: R, klass: KClass<out R> = v::class): R =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.delete(v.table.name), v, klass, klass)

suspend fun <R : TableRecord<R>> WithVertx.dbFetchAsync(v: R, klass: KClass<out R> = v::class): R =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.read(v.table.name), v, klass, klass)

suspend fun <R : TableRecord<R>> WithVertx.dbFindAsync(v: R, klass: KClass<out R> = v::class): List<R> =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableCollectAsync(Address.DB.find(v.table.name), v, klass, klass)

suspend fun WithVertx.dbQueryAsync(q: ComplexDatabaseQuery): List<JsonObject> =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableCollectAsync(Address.DB.query(q.table!!), q,
        ComplexDatabaseQuery::class,
        JsonObject::class)

suspend fun <R : TableRecord<R>> WithVertx.dbQueryAsync(table: Table<R>,
                                                        builderBody: TypedQueryBuilder<R>.() -> Unit) =
    dbQueryAsync(TypedQueryBuilder(table).apply(builderBody).query)

suspend fun WithVertx.dbCountAsync(q: ComplexDatabaseQuery): CountResponse =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.count(q.table!!), q, ComplexDatabaseQuery::class, CountResponse::class)

suspend fun <R : TableRecord<R>> WithVertx.dbCountAsync(table: Table<R>,
                                                        builderBody: TypedQueryBuilder<R>.() -> Unit) =
    dbCountAsync(TypedQueryBuilder(table).apply(builderBody).query)

suspend fun <R : TableRecord<R>> WithVertx.dbProcessAsync(v: R, klass: KClass<out R> = v::class): VerificationData =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.process(v.table.name), v, klass, VerificationData::class)

suspend fun <R : TableRecord<R>> WithVertx.dbVerifyAsync(v: R, klass: KClass<out R> = v::class): VerificationData =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.verify(v.table.name), v, klass, VerificationData::class)

suspend fun <R : TableRecord<R>> WithVertx.dbCleanAsync(v: R, klass: KClass<out R> = v::class): VerificationData =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(Address.DB.clean(v.table.name), v, klass, VerificationData::class)

suspend fun <R : TableRecord<R>> WithVertx.fetchByIdAsync(instance: Table<R>, id: Int,
                                                          klass: KClass<out R> = instance.recordType.kotlin): R =
    @Suppress(DEPRECATION)
    vertx.eventBus().sendJsonableAsync(
        Address.DB.read(instance.name),
        JsonObject("id" to id),
        JsonObject::class, klass)

suspend fun WithVertx.createNotification(record: NotificationRecord) =
        sendJsonable(
                Address.Api.Notification.Create,
                record.fixTitle()
        )

inline suspend fun <
        reified Argument : Any
        > WithVertx.sendJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonable(address, value)
}

inline suspend fun <
        reified Argument : Any
        > WithVertx.publishJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return vertx.eventBus().publishJsonable(address, value)
}

@OptIn(ExperimentalStdlibApi::class)
inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > WithVertx.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableAsync(address, value, typeOf<Argument>(), typeOf<Result>())
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > WithVertx.sendJsonableCollectAsync(address: String, value: Argument): List<Result> {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableCollectAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > WithVertx.trySendJsonableAsync(address: String, value: Argument): Result? {
    return try {
        sendJsonableAsync(address, value)
    } catch (ex: ReplyException) {
        if (ex.failureType() == ReplyFailure.NO_HANDLERS) null
        else throw ex
    }
}
