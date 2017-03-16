package org.jetbrains.research.kotoed.data.teamcity.project

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.EventBusDatum
import org.jetbrains.research.kotoed.eventbus.Address

interface TeamCityRestApi {
    val endpoint: String
        get() = "${Config.TeamCity.EndpointRoot}/$endpointPath"

    val endpointPath: String
}

data class Create(val name: String) : EventBusDatum<Create>, TeamCityRestApi {
    override val address: String
        get() = Address.TeamCity.Create
    override val endpointPath: String
        get() = "projects"

    override fun toJson(): JsonObject = JsonObject.mapFrom(this)

    companion object {
        fun fromJson(json: JsonObject) = json.mapTo(Create::class.java)
    }
}

object Test : EventBusDatum<Test>, TeamCityRestApi {
    override val address: String
        get() = Address.TeamCity.Test
    override val endpointPath: String
        get() = "projects"

    override fun toJson(): JsonObject = JsonObject.mapFrom(this)

    fun fromJson(json: JsonObject) = json.mapTo(Test::class.java)
}
