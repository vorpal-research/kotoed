package org.jetbrains.research.kotoed.config

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.*
import java.io.File
import kotlin.reflect.KProperty
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
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

    @PublishedApi
    internal val KProperty<*>.underscoredName
        get() = camelToKey(name)!!

    operator fun Boolean.getValue(thisRef: Configuration, prop: KProperty<*>): Boolean =
            data.getBoolean(prop.underscoredName, this)

    operator fun String.getValue(thisRef: Configuration, prop: KProperty<*>): String =
            data.getString(prop.underscoredName, this)

    operator fun Int.getValue(thisRef: Configuration, prop: KProperty<*>): Int =
            data.getInteger(prop.underscoredName, this)

    operator fun Long.getValue(thisRef: Configuration, prop: KProperty<*>): Long =
            data.getLong(prop.underscoredName, this)

    operator fun Double.getValue(thisRef: Configuration, prop: KProperty<*>): Double =
            data.getDouble(prop.underscoredName, this)

    operator fun Float.getValue(thisRef: Configuration, prop: KProperty<*>): Float =
            data.getFloat(prop.underscoredName, this)

    operator fun JsonObject.getValue(thisRef: Configuration, prop: KProperty<*>): JsonObject =
            data.getJsonObject(prop.underscoredName, this)

    operator fun JsonArray.getValue(thisRef: Configuration, prop: KProperty<*>): JsonArray =
            data.getJsonArray(prop.underscoredName, this)

    inline operator fun <reified E : Enum<E>> E.getValue(thisRef: Configuration, prop: KProperty<*>): E =
            internalData.getString(prop.underscoredName)?.let { Enum.valueOf<E>(it) } ?: this

    // Nothing? does not work for some reason
    inline operator fun <reified T> Null.getValue(thisRef: Configuration, prop: KProperty<*>): T? =
            internalData.getValueByType(prop.underscoredName, prop.returnType).uncheckedCastOrNull()

    inline operator fun <reified T> Uninitialized.getValue(thisRef: Configuration, prop: KProperty<*>): T =
            internalData.getValueByType(
                    prop.underscoredName,
                    prop.returnType.withNullability(true)
            ).uncheckedCastOrNull<T>() ?:
                    throw IllegalStateException("Configuration field ${prop.name} is not initialized")

    @OptIn(ExperimentalReflectionOnLambdas::class)
    operator fun <T> (() -> T).getValue(thisRef: Configuration, prop: KProperty<*>): T =
            internalData.getValueByType(
                    prop.underscoredName,
                    expectNotNull(this.reflect()).returnType.withNullability(true)
            ).uncheckedCastOrNull() ?: this()

    inline operator fun <reified T : Configuration> T.getValue(thisRef: Configuration, prop: KProperty<*>): T {
        val child = this@getValue
        val root = this@Configuration
        child.internalData = root.internalData.getValue(prop.underscoredName, child.internalData) as JsonObject
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
            ?: File(path).toURI().toURL()
    return JsonObject(file.readText())
}

fun fromVertx(vertx: Vertx, path: String): JsonObject {
    val file = vertx.fileSystem().readFileBlocking(path)
    return JsonObject(file.toString())
}
