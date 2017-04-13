package org.jetbrains.research.kotoed.data.teamcity.build

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.EventBusDatum
import org.jetbrains.research.kotoed.util.JsonableCompanion
import kotlin.reflect.KClass

data class TriggerBuild(
        val id: String,
        val projectId: String,
        val revision: String?
) : EventBusDatum<TriggerBuild>() {
    companion object : JsonableCompanion<TriggerBuild> {
        override val dataklass: KClass<TriggerBuild>
            get() = TriggerBuild::class

        override fun fromJson(json: JsonObject): TriggerBuild? =
                super.fromJson(json)?.apply {
                    if (revision != null && revision.length < 12) {
                        throw IllegalArgumentException(
                                "Revision descriptor $revision is too short"
                        )
                    }
                }
    }
}

data class BuildInfo(
        val id: String?,
        val projectId: String?,
        val number: String?,
        val revision: String?,
        val all: Boolean?
) : EventBusDatum<BuildInfo>()

data class ArtifactCrawl(
        val path: String
) : EventBusDatum<ArtifactCrawl>()

data class ArtifactContent(
        val path: String
) : EventBusDatum<ArtifactContent>()
