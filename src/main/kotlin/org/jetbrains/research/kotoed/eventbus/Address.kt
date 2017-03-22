package org.jetbrains.research.kotoed.eventbus

object Address {
    object TeamCity {
        const val Test = "kotoed.teamcity.test"

        object Project {
            const val Create = "kotoed.teamcity.project.create"
        }

        object Build {
            const val Trigger = "kotoed.teamcity.build.trigger"
            const val Info = "kotoed.teamcity.build.info"
            const val Crawl = "kotoed.teamcity.build.crawl"
            const val Artifact = "kotoed.teamcity.build.artifact"
        }
    }

    object Code {
        const val Download = "kotoed.code.download"
        const val Read = "kotoed.code.read"
    }
}
