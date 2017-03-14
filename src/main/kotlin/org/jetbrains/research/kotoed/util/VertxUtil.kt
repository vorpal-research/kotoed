package org.jetbrains.research.kotoed.util

import io.vertx.core.http.HttpServerRequest
import kotlin.reflect.KProperty

operator fun HttpServerRequest.getValue(thisRef: Nothing?, prop: KProperty<*>) =
        this.getParam(prop.name)
