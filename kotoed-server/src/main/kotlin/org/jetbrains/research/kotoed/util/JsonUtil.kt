@file:Suppress(NOTHING_TO_INLINE)

package org.jetbrains.research.kotoed.util

import com.google.common.base.CaseFormat
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.vertx.core.eventbus.impl.codecs.JsonArrayMessageCodec
import io.vertx.core.eventbus.impl.codecs.JsonObjectMessageCodec
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.warnings.Warnings.NOTHING_TO_INLINE
import kotlinx.warnings.Warnings.UNCHECKED_CAST
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.Field
import org.jooq.Record
import ru.spbstu.ktuples.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

val camelToKey = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE)

/******************************************************************************/

inline fun JsonObject(entries: List<Pair<String, Any?>>) = JsonObject(entries.toMap().toMutableMap())
inline fun JsonObject(vararg entries: Pair<String, Any?>) = JsonObject(entries.toList())
inline fun jsonArrayOf(vararg elements: Any?) = JsonArray(elements.toMutableList())

typealias NullType = Nothing?
typealias JsonEither = EitherOf6<Boolean, Number, String, JsonArray, JsonObject, NullType>

fun Any?.asJsonEither(): JsonEither =
        when (this) {
            is Boolean -> Variant0(this)
            is Number -> Variant1(this)
            is String -> Variant2(this)
            is JsonArray -> Variant3(this)
            is JsonObject -> Variant4(this)
            null -> Variant5(null)
            else -> throw IllegalArgumentException("$this is not a json value")
        }

data class JsonWalker(
        var onBooleanCallBack: (JsonWalker.(Boolean) -> Any?) = { it },
        var onNumberCallBack: (JsonWalker.(Number) -> Any?) = { it },
        var onStringCallBack: (JsonWalker.(String) -> Any?) = { it },
        var onArrayCallBack: (JsonWalker.(JsonArray) -> Any?) = { defaultArrayCallback(it) },
        var onObjectCallBack: (JsonWalker.(JsonObject) -> Any?) = { defaultObjectCallback(it) },
        var onNullCallBack: (JsonWalker.() -> Any?) = { null }
) {
    companion object {
        fun JsonWalker.defaultObjectCallback(obj: JsonObject) =
                JsonObject(obj.map { (k, v) -> k to visit(v) })

        fun JsonWalker.defaultArrayCallback(arr: JsonArray) =
                JsonArray(arr.map { visit(it) })
    }

    fun onBoolean(callback: JsonWalker.(Boolean) -> Any?) {
        onBooleanCallBack = callback
    }

    fun onNumber(callback: JsonWalker.(Number) -> Any?) {
        onNumberCallBack = callback
    }

    fun onString(callback: JsonWalker.(String) -> Any?) {
        onStringCallBack = callback
    }

    fun onNull(callback: JsonWalker.() -> Any?) {
        onNullCallBack = callback
    }

    fun onObject(callback: JsonWalker.(JsonObject) -> Any?) {
        onObjectCallBack = callback
    }

    fun onArray(callback: JsonWalker.(JsonArray) -> Any?) {
        onArrayCallBack = callback
    }

    fun visit(value: Any?): Any? =
            value.asJsonEither()
                    .map0 { onBooleanCallBack(it) }
                    .map1 { onNumberCallBack(it) }
                    .map2 { onStringCallBack(it) }
                    .map3 { onArrayCallBack(it) }
                    .map4 { onObjectCallBack(it) }
                    .map5 { onNullCallBack() }
                    .converge()

}

fun Any?.walk(f: JsonWalker.() -> Unit) =
        JsonWalker().apply { f() }.visit(this)

/******************************************************************************/

inline operator fun JsonArray.component1(): Any? = this.getValue(0)
inline operator fun JsonArray.component2(): Any? = this.getValue(1)
inline operator fun JsonArray.component3(): Any? = this.getValue(2)
inline operator fun JsonArray.component4(): Any? = this.getValue(3)

inline operator fun JsonArray.get(index: Int): Any? = this.getValue(index)
inline operator fun JsonArray.set(index: Int, value: Any?) = this.list.set(index, value)
inline operator fun JsonObject.get(key: String): Any? = this.getValue(camelToKey(key))
inline operator fun JsonObject.set(key: String, value: Any?) = this.put(camelToKey(key), value)
inline operator fun JsonObject.contains(key: String) = this.containsKey(camelToKey(key))


/******************************************************************************/

inline operator fun <T> JsonObject.get(field: Field<T>): T? =
        this.get(field.name).uncheckedCastOrNull()


/******************************************************************************/

inline fun JsonObject.rename(oldName: String, newName: String): JsonObject =
        if (containsKey(camelToKey(oldName))) {
            put(camelToKey(newName), remove(camelToKey(oldName)))
        } else this

inline fun JsonObject.retainFields(vararg fields: String): JsonObject =
        this.map.keys.retainAll(fields.map { camelToKey(it)!! }).let { this }

inline fun JsonObject.removeFields(vararg fields: String): JsonObject =
        this.map.keys.removeAll(fields.map { camelToKey(it)!! }).let { this }

inline operator fun JsonObject.get(fields: List<String>) =
        fields.dropLast(1).fold(this) { obj, key_ ->
            obj.getJsonObject(camelToKey(key_)!!)
        }.get(fields.last())

inline fun JsonObject?.safeNav(fields: List<String>) =
        fields.dropLast(1).fold(this) { obj, key_ ->
            obj?.getJsonObject(camelToKey(key_)!!)
        }?.get(fields.last())

inline fun JsonObject?.safeNav(vararg fields: String) =
        safeNav(fields.asList())

inline fun <reified T> JsonObject?.safeNavAs(vararg fields: String) =
        safeNav(fields.asList()) as? T

inline operator fun JsonObject.get(vararg fields: String) =
        get(fields.asList())

inline operator fun JsonObject.set(fields: List<String>, value: Any?) =
        fields.dropLast(1).fold(this) { obj, key_ ->
            val key = camelToKey(key_)!!
            when {
                key in obj -> obj.getJsonObject(key)
                        ?: throw IllegalArgumentException("JSON field $key: object expected")
                else -> JsonObject().apply { obj.put(key, this@apply) }
            }
        }.set(fields.last(), value).let { this }

inline operator fun JsonObject.set(vararg fields: String, value: Any?) =
        set(fields.asList(), value)

fun JsonObject.snakeKeys(): JsonObject {
    fun recurse(value: Any?): Any? = when (value) {
        is JsonObject -> value.snakeKeys()
        is JsonArray -> value.map { recurse(it) }.let(::JsonArray)
        else -> value
    }
    return JsonObject(this.asSequence().map { (k, v) ->
        camelToKey(k) to recurse(v)
    }.toMap())
}


/******************************************************************************/

val declaredMemberPropertyCache: LoadingCache<KClass<*>, Collection<KProperty1<*, *>>> = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(
                        object : CacheLoader<KClass<*>, Collection<KProperty1<*, *>>>() {
                            override fun load(key: KClass<*>): Collection<KProperty1<*, *>> {
                                return key.declaredMemberProperties
                            }
                        }
                )

/******************************************************************************/

interface Jsonable {
    fun toJson(): JsonObject =
            JsonObject(declaredMemberPropertyCache[javaClass.kotlin].map { Pair(camelToKey(it.name)!!, it.call(this).tryToJson()) })
}

interface JsonableCompanion<T : Any> {
    val dataklass: KClass<T>

    fun fromJson(json: JsonObject): T? = objectFromJson(json, dataklass.starProjectedType)
}

// marker interfaces for remote communications
interface SerializedAsObject<K, V>
interface SerializedAsArray<E>

/******************************************************************************/

@PublishedApi
internal fun Any?.tryToJson(): Any? =
        when (this) {
            null -> null
            is Jsonable -> toJson()
            is Record -> toJson<Record>()
            is JsonObject -> this
            is JsonArray -> this
            is Collection<*> -> JsonArray(this.map { it.tryToJson() })
            is Iterable<*> -> JsonArray(this.map { it.tryToJson() })
            is Sequence<*> -> JsonArray(this.map { it.tryToJson() }.toList())
            is Map<*, *> -> JsonArray(this.map { it.tryToJson() })
            is Map.Entry<*, *> -> jsonArrayOf(key.tryToJson(), value.tryToJson())
            is Pair<*, *> -> jsonArrayOf(first.tryToJson(), second.tryToJson())
            is Triple<*, *, *> -> jsonArrayOf(first.tryToJson(), second.tryToJson(), third.tryToJson())
            is Tuple -> JsonArray(toArray().map { it.tryToJson() })
            is VariantBase -> JsonObject("index" to index, "value" to value.tryToJson())
            is Number, is String, is Boolean -> this
            is CharSequence -> toString()
            is Enum<*> -> toString()
            is Date -> time
            is Instant -> this.toEpochMilli()
            is LocalDateTime -> this.toInstant(ZoneOffset.UTC).toEpochMilli()
            is OffsetDateTime -> this.toInstant().toEpochMilli()
            else -> throw IllegalArgumentException("Cannot convert $this to json")
        }

inline fun jsonValue(value: Any?): Any? = value.tryToJson()

private fun makeJsonCollection(klass: KType, list: List<Any?>): Any =
        when (klass.jvmErasure) {
            List::class, Collection::class, Iterable::class -> list
            Sequence::class -> list.asSequence()
            Set::class -> list.toSet()
            Map::class -> list.filterIsInstance<Pair<*, *>>().toMap()
            else -> throw IllegalArgumentException("Cannot convert json array $list to type $klass")
        }

/******************************************************************************/

val subclassCache: LoadingCache<Pair<KClass<*>, KClass<*>>, Boolean> = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build(
                object : CacheLoader<Pair<KClass<*>, KClass<*>>, Boolean>() {
                    override fun load(key: Pair<KClass<*>, KClass<*>>): Boolean {
                        val (erasure, target) = key
                        return erasure.isSubclassOf(target)
                    }
                }
        )

private fun Any?.tryFromJson(klass: KType): Any? {

    fun KClass<*>.isSubclassOf(target: KClass<*>) = subclassCache[this to target]

    fun die(): Nothing = throw IllegalArgumentException("Cannot convert $this from json to type $klass")
    val erasure = klass.jvmErasure
    val companion = erasure.companionObjectInstance
    return when (this) {
        in Equals(null) and Guard { klass.isMarkedNullable } -> null
        is JsonObject -> {
            when {
                erasure.isSubclassOf(JsonObject::class) -> this
                erasure.isSubclassOf(VariantBase::class) -> {
                    val index = this.getInteger("index") ?: die()
                    val value = this.getJsonObject("value") ?: die()
                    Variant(index, value.tryFromJson(klass.arguments[index].type!!))
                }
                companion is JsonableCompanion<*> -> companion.fromJson(this)
                klass.jvmErasure.isSubclassOf(Jsonable::class) -> objectFromJson(this, klass)
                klass.jvmErasure.isSubclassOf(Record::class) ->
                    toRecord<Record>(klass.jvmErasure.uncheckedCast<KClass<Record>>())
                else -> die()
            }
        }
        is JsonArray ->
            when {
                erasure.isSubclassOf(JsonArray::class) -> this
                erasure.isSubclassOf(Collection::class)
                        || erasure.isSubclassOf(Iterable::class)
                        || erasure.isSubclassOf(Sequence::class) -> {
                    val elementType = klass.arguments.first().type ?: die()
                    makeJsonCollection(klass, this@tryFromJson.map { it.tryFromJson(elementType) })
                }
                erasure.isSubclassOf(Map::class) -> {
                    val (keyArg, valueArg) = klass.arguments
                    val keyType = keyArg.type ?: die()
                    val valueType = valueArg.type ?: die()
                    makeJsonCollection(klass, this@tryFromJson.map {
                        it.tryFromJson(
                                Pair::class.createType(
                                        listOf(
                                                KTypeProjection.invariant(keyType),
                                                KTypeProjection.invariant(valueType)
                                        )
                                )
                        )
                    })
                }
                erasure == Pair::class -> {
                    val (firstArg, secondArg) = klass.arguments
                    val (first, second) = this
                    Pair(
                            first.tryFromJson(firstArg.type ?: die()),
                            second.tryFromJson(secondArg.type ?: die())
                    )
                }
                erasure == Triple::class -> {
                    val (firstArg, secondArg, thirdArg) = klass.arguments
                    val (first, second, third) = this
                    Triple(
                            first.tryFromJson(firstArg.type ?: die()),
                            second.tryFromJson(secondArg.type ?: die()),
                            third.tryFromJson(thirdArg.type ?: die())
                    )
                }
                erasure.isSubclassOf(Tuple::class) -> {
                    val elementTypes = klass.arguments
                    val elements = this as Iterable<Any?>
                    (elementTypes zip elements).map { (t, v) ->
                        v.tryFromJson(t.type ?: die())
                    }.let { Tuple.ofList(it) }
                }
                else -> die()
            }
        is Boolean -> this
        is String ->
            when {
                erasure.isSubclassOf(Enum::class) -> Enum.valueOf(this, erasure)
                erasure.isSuperclassOf(String::class) -> this
                else -> die()
            }
        is Number ->
            when (erasure) {
                Int::class -> toInt()
                Long::class -> toLong()
                Short::class -> toShort()
                Byte::class -> toByte()
                Float::class -> toFloat()
                Double::class -> toDouble()

                Instant::class -> Instant.ofEpochMilli(toLong())
                Date::class -> Date.from(Instant.ofEpochMilli(toLong()))
                LocalDateTime::class -> LocalDateTime.ofInstant(Instant.ofEpochMilli(toLong()), ZoneOffset.UTC)
                OffsetDateTime::class -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(toLong()), ZoneOffset.UTC)

                else -> die()
            }
        else -> die()
    }
}

private fun <T : Any> objectFromJson(data: JsonObject, ktype: KType): T {
    val klass: KClass<T> = ktype.jvmErasure.uncheckedCast()
    val pmapping = ktype.parameterMapping
    if (klass.isSealed && klass.isSubclassOf(JsonableSealed::class)) return sealedFromJson(data, klass)
    val asMap = declaredMemberPropertyCache[klass].map {
        val key = camelToKey(it.name)
        val value = data.getValue(key)
        if (value == null && !it.returnType.isMarkedNullable)
            throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: required field ${it.name} is missing")
        else Pair(it.name, value.tryFromJson(it.returnType.applyMapping(pmapping)))
    }.toMap()

    return try {
        fun ctorCheck(ctor: KFunction<*>) = ctor.parameters.map { it.name }.toSet() == asMap.keys
        val ctor = klass.primaryConstructor?.takeIf(::ctorCheck)
                ?: klass.constructors.find(::ctorCheck)
        ctor!!.callBy(asMap.mapKeys { prop -> ctor.parameters.find { param -> param.name == prop.key }!! })
    } catch (ex: Exception) {
        throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: please use only datatype-like classes", ex)
    }
}

const val classField = "#class"

interface JsonableSealed : Jsonable {
    override fun toJson(): JsonObject =
            super.toJson().apply { set(classField, this@JsonableSealed::class.simpleName) }
}

private fun <T : Any> sealedFromJson(data: JsonObject, klass: KClass<T>): T {
    require(klass.isSealed)
    val descendants = klass.nestedClasses.filter {
        it.isSubclassOf(klass)
    }.map { it.simpleName!! to it.uncheckedCast<KClass<out T>>() }.toMap()

    val discriminator = data.getString(classField, null)
            ?: throw IllegalArgumentException("No discriminator field while trying to parse $klass")
    val childClass = descendants[discriminator]
            ?: throw IllegalArgumentException("Unknown child class: $discriminator")

    return objectFromJson(data, childClass.starProjectedType)
}

/******************************************************************************/

fun <T : Any> fromJson(data: JsonObject, kclass: KClass<T>): T = fromJson(data, kclass.starProjectedType)
fun <T : Any> fromJson(data: JsonObject, ktype: KType): T {
    @Suppress(UNCHECKED_CAST)
    val klass = ktype.jvmErasure as KClass<T>
    if (klass.isSubclassOf(JsonObject::class)) return data.uncheckedCast<T>()
    klass.staticFunctions.firstOrNull { it.name == "fromJson" }?.let {
        return it.call(data).uncheckedCast<T>()
    }

    val companion = klass.companionObjectInstance
    return when (companion) {
        is JsonableCompanion<*> -> klass.safeCast(companion.fromJson(data))
                ?: throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: companion method failed")
        else -> objectFromJson(data, ktype)
    }
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> fromJson(data: JsonObject) = fromJson<T>(data, typeOf<T>())
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Jsonable> JsonObject.toJsonable() = fromJson<T>(this, typeOf<T>())

/******************************************************************************/

fun JsonObject.getValueByType(name: String, type: KType): Any? {
    val value = getValue(camelToKey(name))
    if (value == null && !type.isMarkedNullable)
        throw IllegalArgumentException("Field $name is missing in \"${this}\"")
    return value?.tryFromJson(type)
}

/******************************************************************************/

object AnyAsJson {
    operator fun Any?.get(key: String) = (this as? JsonObject)?.run {
        getValue(key) ?: getValue(camelToKey(key))
    }

    operator fun Any?.get(index: Int) = (this as? JsonArray)?.getValue(index)
}

data class JsonDelegate(val obj: JsonObject) {
    operator fun <T> getValue(thisRef: Any?, prop: KProperty<*>) = obj.getValue(camelToKey(prop.name)!!).uncheckedCast<T>()
    operator fun <T> setValue(thisRef: Any?, prop: KProperty<*>, value: T) = obj.set(camelToKey(prop.name)!!, value)
}

val JsonObject.delegate get() = JsonDelegate(this)

/******************************************************************************/

object JsonEx

fun JsonEx.decode(enc: String): Any? = JsonArray("""[$enc]""").getValue(0)

/******************************************************************************/

object NonCopyJsonObjectCodec : JsonObjectMessageCodec(), Loggable {
    override fun name() = "non-copy-object"
    override fun transform(jsonObject: JsonObject): JsonObject = jsonObject
    override fun systemCodecID(): Byte = -1
}

object NonCopyJsonArrayCodec : JsonArrayMessageCodec(), Loggable {
    override fun name() = "non-copy-array"
    override fun transform(jsonObject: JsonArray): JsonArray = jsonObject
    override fun systemCodecID(): Byte = -1
}
