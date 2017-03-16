package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

inline operator fun <reified T> JsonObject.getValue(thisRef: Any?, property: KProperty<*>): T? =
        this.getValue(property.name) as T?

inline fun JsonObject(vararg entries: Pair<String, Any?>) = JsonObject(entries.toMap())
inline fun JsonArray(vararg elements: Any?) = JsonArray(elements.asList())

inline operator fun JsonArray.component1(): Any? = this.getValue(0)
inline operator fun JsonArray.component2(): Any? = this.getValue(1)
inline operator fun JsonArray.component3(): Any? = this.getValue(2)
inline operator fun JsonArray.component4(): Any? = this.getValue(3)

interface Jsonable

fun Jsonable.toJson() =
        JsonObject(javaClass.kotlin.declaredMemberProperties.map { Pair(it.name, it.call(this).tryToJson()) }.toMap())

private fun Any?.tryToJson(): Any? =
        when (this) {
            null -> null
            is Jsonable -> toJson()
            is Collection<*> -> JsonArray(this.map { it.tryToJson() })
            is Map<*, *> -> JsonArray(this.map { it.tryToJson() })
            is Map.Entry<*, *> -> JsonArray(key.tryToJson(), value.tryToJson())
            is Pair<*, *> -> JsonArray(first.tryToJson(), second.tryToJson())
            is Triple<*, *, *> -> JsonArray(first.tryToJson(), second.tryToJson(), third.tryToJson())
            is Number, is String -> this
            else -> throw IllegalArgumentException("Cannot convert $this to json")
        }

private fun makeJsonCollection(klass: KType, list: List<Any?>): Any =
        when (klass.jvmErasure) {
            List::class, Collection::class, Iterable::class -> list
            Set::class -> list.toSet()
            Map::class -> list.map { it as Pair<*, *> }.toMap()
            Sequence::class -> list.asSequence()
            else -> throw IllegalArgumentException("Cannot convert json array $list to type $klass")
        }

private fun Any?.tryFromJson(klass: KType): Any? {
    val die = { throw IllegalArgumentException("Type $klass not supported") }
    return when (this) {
        is JsonObject -> fromJson(this, klass.jvmErasure)
        is JsonArray ->
            when {
                klass.jvmErasure.isSubclassOf(Collection::class)
                        || klass.jvmErasure.isSubclassOf(Sequence::class) -> {
                    val elementType = klass.arguments.first().type ?: die()
                    makeJsonCollection(klass, this@tryFromJson.map { it.tryFromJson(elementType) })
                }
                klass.jvmErasure.isSubclassOf(Map::class) -> {
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
                klass.jvmErasure == Pair::class -> {
                    val (firstArg, secondArg) = klass.arguments
                    val (first, second) = this
                    Pair(
                            first.tryFromJson(firstArg.type ?: die()),
                            second.tryFromJson(secondArg.type ?: die())
                    )
                }
                klass.jvmErasure == Triple::class -> {
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
        is Number, is String -> this
        else -> throw IllegalArgumentException("Cannot convert $this from json as $klass")
    }
}

fun <T : Any> fromJson(data: JsonObject, klass: KClass<T>): T {
    val asArray = klass.declaredMemberProperties.map {
        val value = data.getValue(it.name)
        if (value == null && !it.returnType.isMarkedNullable)
            throw IllegalArgumentException("Cannot convert \"$data\" to type $klass: required field ${it.name} is missing")
        else value?.tryFromJson(it.returnType)
    }.toTypedArray()

    return try {
        klass.constructors.first().call(*asArray)
    } catch (ex: Exception) {
        throw IllegalArgumentException("Cannot construct type $klass from \"$data\"; please use only datatype-like classes", ex)
    }
}

inline fun <reified T : Any> fromJson(data: JsonObject) = fromJson(data, T::class)
