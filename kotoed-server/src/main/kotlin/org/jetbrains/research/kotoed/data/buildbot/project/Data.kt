package org.jetbrains.research.kotoed.data.buildbot.project

import org.jetbrains.research.kotoed.data.EventBusDatum

data class CreateProject(
        val id: Int,
        val courseName: String,
        val name: String,
        val repoUrl: String,
        val repoType: String
) : EventBusDatum<CreateProject>()
