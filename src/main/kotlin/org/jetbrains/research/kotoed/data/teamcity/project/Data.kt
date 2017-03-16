package org.jetbrains.research.kotoed.data.teamcity.project

import org.jetbrains.research.kotoed.data.EventBusDatum

data class Create(val name: String) : EventBusDatum<Create>()

object Test : EventBusDatum<Test>()
