@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.research.kotoed.util

import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

/******************************************************************************/

inline operator fun <reified T> JsonObject.getValue(thisRef: Any?, property: KProperty<*>): T? =
        this.getValue(property.name) as T?

inline fun JsonObject(entries: List<Pair<String, Any?>>) = JsonObject(entries.toMap().toMutableMap())
inline fun JsonObject(vararg entries: Pair<String, Any?>) = JsonObject(entries.toList())
inline fun jsonArrayOf(vararg elements: Any?) = JsonArray(elements.toMutableList())

/******************************************************************************/

inline operator fun JsonArray.component1(): Any? = this.getValue(0)
inline operator fun JsonArray.component2(): Any? = this.getValue(1)
inline operator fun JsonArray.component3(): Any? = this.getValue(2)
inline operator fun JsonArray.component4(): Any? = this.getValue(3)

inline operator fun JsonArray.get(index: Int): Any? = this.getValue(index)
inline operator fun JsonArray.set(index: Int, value: Any?) = this.list.set(index, value)
inline operator fun JsonObject.get(key: String): Any? = this.getValue(key)
inline operator fun JsonObject.set(key: String, value: Any?) = this.put(key, value)

/******************************************************************************/

inline fun JsonObject.rename(oldName: String, newName: String): JsonObject =
        if (containsKey(oldName)) {
            put(newName, remove(oldName))
        } else this

/******************************************************************************/

interface Jsonable {
    fun toJson(): JsonObject =
            JsonObject(javaClass.kotlin.declaredMemberProperties.map { Pair(it.name, it.call(this).tryToJson()) })
}

interface JsonableCompanion<T : Any> {
    val dataklass: KClass<T>

    fun fromJson(json: JsonObject): T? = objectFromJson(json, dataklass)
}

/******************************************************************************/

private fun Any?.tryToJson(): Any? =
        when (this) {
            null -> null
            is Jsonable -> toJson()
            is JsonObject -> this
            is JsonArray -> this
            is Collection<*> -> JsonArray(this.map { it.tryToJson() })
            is Iterable<*> -> JsonArray(this.map { it.tryToJson() })
            is Sequence<*> -> JsonArray(this.map { it.tryToJson() }.toList())
            is Map<*, *> -> JsonArray(this.map { it.tryToJson() })
            is Map.Entry<*, *> -> jsonArrayOf(key.tryToJson(), value.tryToJson())
            is Pair<*, *> -> jsonArrayOf(first.tryToJson(), second.tryToJson())
            is Triple<*, *, *> -> jsonArrayOf(first.tryToJson(), second.tryToJson(), third.tryToJson())
            is Number, is String, is Boolean -> this
            is Enum<*> -> toString()
            else -> throw IllegalArgumentException("Cannot convert $this to json")
        }

private fun makeJsonCollection(klass: KType, list: List<Any?>): Any =
        when (klass.jvmErasure) {
            List::class, Collection::class, Iterable::class -> list
            Sequence::class -> list.asSequence()
            Set::class -> list.toSet()
            Map::class -> list.map { it as Pair<*, *> }.toMap()
            else -> throw IllegalArgumentException("Cannot convert json array $list to type $klass")
        }

/******************************************************************************/

private fun Any?.tryFromJson(klass: KType): Any? {
    fun die(): Nothing = throw IllegalArgumentException("Cannot convert $this from json to type $klass")
    val erasure = klass.jvmErasure
    val companion = erasure.companionObjectInstance
    return when (this) {
        is JsonObject -> {
            when {
                erasure.isSubclassOf(JsonObject::class) -> this
                companion is JsonableCompanion<*> -> companion.fromJson(this)
                klass.jvmErasure.isSubclassOf(Jsonable::class) -> objectFromJson(this, erasure)
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
                else -> die()
            }
        is Boolean -> this
        is String ->
            when {
                erasure.isSubclassOf(Enum::class) -> Enum.valueOf(this, erasure)
                erasure.isSubclassOf(String::class) -> this
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
                else -> die()
            }
        else -> die()
    }
}

private fun <T : Any> objectFromJson(data: JsonObject, klass: KClass<T>): T {
    val asMap = klass.declaredMemberProperties.map {
        val value = data.getValue(it.name)
        if (value == null && !it.returnType.isMarkedNullable)
            throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: required field ${it.name} is missing")
        else Pair(it.name, value?.tryFromJson(it.returnType))
    }.toMap()

    return try {
        val ctor = klass.constructors.first()
        ctor.callBy(asMap.mapKeys { prop -> ctor.parameters.find { param -> param.name == prop.key }!! })
    } catch (ex: Exception) {
        throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: please use only datatype-like classes", ex)
    }
}

/******************************************************************************/

@Suppress("UNCHECKED_CAST")
fun <T : Any> fromJson(data: JsonObject, klass: KClass<T>): T {
    if (klass.isSubclassOf(JsonObject::class)) return data as T

    val companion = klass.companionObjectInstance
    return when (companion) {
        is JsonableCompanion<*> -> companion.fromJson(data) as? T
                ?: throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: companion method failed")
        else -> objectFromJson(data, klass)
    }
}

inline fun <reified T : Any> fromJson(data: JsonObject) = fromJson(data, T::class)

/******************************************************************************/

fun JsonObject.getValueByType(name: String, type: KType): Any? {
    val value = getValue(name)
    if (value == null && !type.isMarkedNullable)
        throw IllegalArgumentException("Field $name is missing in \"${this}\"")
    return value?.tryFromJson(type)
}

/******************************************************************************/

object AnyAsJson {
    operator fun Any?.get(key: String) = (this as? JsonObject)?.getValue(key)
    operator fun Any?.get(index: Int) = (this as? JsonArray)?.getValue(index)
}

data class JsonDelegate(val obj: JsonObject) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> getValue(thisRef: Any?, prop: KProperty<*>) = obj.getValue(prop.name) as T

    operator fun <T> setValue(thisRef: Any?, prop: KProperty<*>, value: T) = obj.set(prop.name, value)
}

val JsonObject.delegate get() = JsonDelegate(this)


/******************************************************************************/

object JsonEx
fun JsonEx.decode(enc: String): Any? = JsonArray("""[$enc]""").getValue(0)

/******************************************************************************/
