package org.jetbrains.research.kotoed.util

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlin.reflect.KProperty

inline operator fun<reified T> JsonObject.getValue(thisRef: Any?, property: KProperty<*>): T? =
        this.getValue(property.name) as T?
