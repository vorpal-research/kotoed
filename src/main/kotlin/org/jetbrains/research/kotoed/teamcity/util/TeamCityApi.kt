package org.jetbrains.research.kotoed.teamcity.util

data class ApiEndpoint(val path: String) {
    operator fun invoke(): String = path
}

interface Locator

object EmptyLocator : Locator {
    override fun toString(): String = ""
}

data class StringLocator(val data: String) : Locator {
    override fun toString(): String = "$data"
}

data class DimensionLocator(val dimension: String, val value: String) : Locator {
    override fun toString(): String = "$dimension:$value"

    companion object {
        fun from(dimension: String, value: String?) =
                if (value == null) EmptyLocator else DimensionLocator(dimension, value)
    }
}

operator infix fun ApiEndpoint.plus(locator: Locator) = ApiEndpoint("$path/$locator")()

operator infix fun Locator.times(locator: Locator) =
        if (this is EmptyLocator) locator
        else if (locator is EmptyLocator) this
        else StringLocator("$this,$locator")

fun Locator.all() =
        if (this is EmptyLocator) this else StringLocator("?locator=$this")

object TeamCityApi {
    private val endpointRoot = "/app/rest"

    private operator fun String.unaryPlus() = ApiEndpoint("$endpointRoot/$this")

    val Projects = +"projects"
    val VcsRoots = +"vcs-roots"
    val BuildTypes = +"buildTypes"
    val BuildQueue = +"buildQueue"
    val Builds = +"builds"
}
