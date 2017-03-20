package org.jetbrains.research.kotoed.data.teamcity.project

import org.jetbrains.research.kotoed.data.EventBusDatum

data class Project(
        val id: String,
        val name: String,
        val rootProjectId: String) : EventBusDatum<Project>()

data class VcsRoot(
        val id: String,
        val name: String,
        val type: String,
        val url: String,
        val projectId: String) : EventBusDatum<VcsRoot>()

data class BuildConfig(
        val id: String,
        val name: String,
        val templateId: String) : EventBusDatum<BuildConfig>()

data class CreateProject(
        val project: Project,
        val vcsRoot: VcsRoot,
        val buildConfig: BuildConfig
) : EventBusDatum<CreateProject>()

data class TriggerBuild(val id: String) : EventBusDatum<TriggerBuild>()

data class BuildInfo(
        val id: String?,
        val projectId: String?,
        val number: String?,
        val revision: String?,
        val all: Boolean?
) : EventBusDatum<BuildInfo>()
