package org.jetbrains.research.kotoed.eventbus

object Address {
    object TeamCity {
        const val Test = "kotoed.teamcity.test"

        object Project {
            const val Create = "kotoed.teamcity.project.create"
        }
    }
}
