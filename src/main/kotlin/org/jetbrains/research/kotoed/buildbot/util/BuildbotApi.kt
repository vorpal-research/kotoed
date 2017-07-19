package org.jetbrains.research.kotoed.buildbot.util

import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.Jsonable

data class ApiEndpoint(val path: String) : Jsonable {
    operator fun invoke(): String = path
}

interface Locator : Jsonable

object EmptyLocator : Locator {
    override fun toString(): String = ""
}

data class StringLocator(val data: String) : Locator {
    override fun toString(): String = "$data"
}

data class DimensionLocator(val dimension: String, val value: String) : Locator {
    override fun toString(): String = "$dimension/$value"

    companion object {
        fun from(dimension: String, value: String?) =
                if (value == null) EmptyLocator else DimensionLocator(dimension, value)

        fun from(dimension: String, value: Any?) =
                if (value == null) EmptyLocator else DimensionLocator(dimension, value.toString())
    }
}

operator infix fun ApiEndpoint.plus(locator: Locator) = ApiEndpoint("$path/$locator")()

operator infix fun Locator.times(locator: Locator) =
        if (this is EmptyLocator) locator
        else if (locator is EmptyLocator) this
        else StringLocator("$this,$locator")

operator infix fun Locator.div(locator: Locator) =
        if (this is EmptyLocator) locator
        else if (locator is EmptyLocator) this
        else StringLocator("$this/$locator")

object BuildbotApi {
    private val endpointRoot = Config.Buildbot.EndpointRoot

    private operator fun String.unaryPlus() = ApiEndpoint("$endpointRoot/$this")
    private operator fun String.unaryMinus() = StringLocator(this)

    val Empty = "/"
    val Root = ApiEndpoint("$endpointRoot")
    val ForceSchedulers = +"forceschedulers"
}
