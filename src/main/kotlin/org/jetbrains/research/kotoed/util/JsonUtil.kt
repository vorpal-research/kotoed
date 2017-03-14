package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonObject
import kotlin.reflect.KProperty

operator fun JsonObject.getValue(thisRef: Any?, property: KProperty<*>): Any? =
        this.getValue(property.name)
