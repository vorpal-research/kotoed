package org.jetbrains.research.kotoed.config

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.getValueByType
import org.jetbrains.research.kotoed.util.toJson
import kotlin.reflect.KProperty
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.reflect

object Uninitialized
object Null

abstract class Configuration : Jsonable {
    private var data: JsonObject = JsonObject()
    var internalData
        get() = data
        set(value) {
            data = value
        }

    operator fun String.getValue(thisRef: Configuration, prop: KProperty<*>): String =
            data.getString(prop.name, this)

    operator fun Int.getValue(thisRef: Configuration, prop: KProperty<*>): Int =
            data.getInteger(prop.name, this)

    operator fun Long.getValue(thisRef: Configuration, prop: KProperty<*>): Long =
            data.getLong(prop.name, this)

    operator fun Double.getValue(thisRef: Configuration, prop: KProperty<*>): Double =
            data.getDouble(prop.name, this)

    operator fun Float.getValue(thisRef: Configuration, prop: KProperty<*>): Float =
            data.getFloat(prop.name, this)

    operator fun JsonObject.getValue(thisRef: Configuration, prop: KProperty<*>): JsonObject =
            data.getJsonObject(prop.name, this)

    operator fun JsonArray.getValue(thisRef: Configuration, prop: KProperty<*>): JsonArray =
            data.getJsonArray(prop.name, this)

    // Nothing? does not work for some reason
    inline operator fun <reified T> Null.getValue(thisRef: Configuration, prop: KProperty<*>): T? =
            internalData.getValueByType(prop.name, prop.returnType) as? T

    inline operator fun <reified T> Uninitialized.getValue(thisRef: Configuration, prop: KProperty<*>): T =
            internalData.getValueByType(prop.name, prop.returnType.withNullability(true)) as? T ?:
                    throw IllegalStateException("Configuration field ${prop.name} is not initialized")

    @Suppress("UNCHECKED_CAST")
    operator fun <T> (() -> T).getValue(thisRef: Configuration, prop: KProperty<*>): T =
            internalData.getValueByType(prop.name, this.reflect()?.returnType?.withNullability(true)!!) as? T ?: this()

    inline operator fun <reified T : Configuration> T.getValue(thisRef: Configuration, prop: KProperty<*>): T {
        val child = this@getValue
        val root = this@Configuration
        child.internalData = root.internalData.getValue(prop.name, child.internalData) as JsonObject
        return child
    }

    override fun toString(): String {
        return toJson().encodePrettily()
    }
}

fun <T : Configuration> loadConfiguration(base: T, vararg json: JsonObject): T {
    val data = JsonObject()
    for (obj in json) {
        data.mergeIn(obj, true)
    }
    base.internalData = data
    return base
}

fun <T : Configuration> T.mergeIn(override: T) = loadConfiguration(this, internalData, override.internalData)

fun fromResource(path: String): JsonObject {
    val file = Configuration::class.java.classLoader.getResource(path)
    return JsonObject(file.readText())
}

fun fromVertx(vertx: Vertx, path: String): JsonObject {
    val file = vertx.fileSystem().readFileBlocking(path)
    return JsonObject(file.toString())
}
