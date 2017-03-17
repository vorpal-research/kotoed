package org.jetbrains.research.kotoed.teamcity.util

object TeamCityApi {
    private val endpointRoot = "/app/rest"

    private operator fun String.unaryPlus() = "$endpointRoot/$this"

    val Projects = +"projects"
    val VcsRoots = +"vcs-roots"
    val BuildTypes = +"buildTypes"
    val BuildQueue = +"buildQueue"
}
