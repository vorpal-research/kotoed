package org.jetbrains.research.kotoed.data.teamcity.build

import org.jetbrains.research.kotoed.data.EventBusDatum

data class TriggerBuild(val id: String) : EventBusDatum<TriggerBuild>()

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
