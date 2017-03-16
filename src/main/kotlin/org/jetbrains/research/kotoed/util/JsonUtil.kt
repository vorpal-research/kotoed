package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonObject
import kotlin.reflect.KProperty

operator fun JsonObject.getValue(thisRef: Any?, property: KProperty<*>) =
        this.getValue(property.name)

interface Jsonable {
    fun toJson(): JsonObject
}

fun JsonObjectOf(vararg pairs: Pair<Any?, Any?>): JsonObject =
        JsonObject.mapFrom(mapOf(*pairs))
