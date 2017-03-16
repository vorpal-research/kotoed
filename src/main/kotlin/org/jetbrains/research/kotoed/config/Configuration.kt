package org.jetbrains.research.kotoed.config

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.toJson
import kotlin.reflect.KProperty

object Uninitialized
object Null

abstract class Configuration: Jsonable {
    private var data: JsonObject = JsonObject()
    var internalData
        get() = data
        set(value){ data = value }

    operator fun String.getValue(thisRef: Configuration, prop: KProperty<*>): String {
        return data.getValue(prop.name, this) as String
    }
    operator fun Int.getValue(thisRef: Configuration, prop: KProperty<*>) = data.getValue(prop.name, this) as Int
    operator fun Double.getValue(thisRef: Configuration, prop: KProperty<*>) = data.getValue(prop.name, this) as Double

    // Nothing? does not work for some reason
    inline operator fun<reified T> Null.getValue(thisRef: Configuration, prop: KProperty<*>): T {
        return internalData.getValue(prop.name, this) as T
    }
    inline operator fun<reified T> Uninitialized.getValue(thisRef: Configuration, prop: KProperty<*>): T {
        return internalData.getValue(prop.name) as? T ?:
                throw IllegalStateException("Configuration field ${prop.name} is not initialized")
    }

    @Suppress("UNCHECKED_CAST")
    operator fun<T> (() -> T).getValue(thisRef: Configuration, prop: KProperty<*>): T {
        return internalData.getValue(prop.name) as? T ?: this()
    }

    fun dbg(ret: Configuration, prop: KProperty<*>) {
        ret.internalData = internalData.getValue(prop.name, ret.internalData) as JsonObject
    }

    inline operator fun <reified T: Configuration> T.getValue(thisRef: Configuration, prop: KProperty<*>): T {
        val ret = this@getValue
        this@Configuration.dbg(ret, prop)
        return ret
    }

    override fun toString(): String {
        return toJson().encodePrettily()
    }
}

fun <T: Configuration> loadConfiguration(base: T, vararg json: JsonObject): T {
    val data = JsonObject()
    for(obj in json) {
        data.mergeIn(obj, true)
    }
    base.internalData = data
    return base
}

fun fromResource(path: String): JsonObject {
    val file = Configuration::class.java.classLoader.getResource(path)
    return JsonObject(file.readText())
}