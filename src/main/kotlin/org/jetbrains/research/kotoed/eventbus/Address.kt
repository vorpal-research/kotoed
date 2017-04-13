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
        const val Ping = "kotoed.code.ping"
        const val Download = "kotoed.code.download"
        const val Read = "kotoed.code.read"
        const val List = "kotoed.code.list"
        const val Diff = "kotoed.code.diff"
        const val LocationDiff = "kotoed.code.diff.location"
    }

    object User {
        object Auth {
            const val SignUp = "kotoed.user.auth.signup"
            const val Login = "kotoed.user.auth.login"
            const val Info = "kotoed.user.auth.info"
        }
    }

    object DB {
        fun create(entity: String) = "kotoed.db.$entity.create"
        fun delete(entity: String) = "kotoed.db.$entity.delete"
        fun read(entity: String) = "kotoed.db.$entity.read"
        fun readFor(entity: String, key: String) = "${read(entity)}.for.$key"
        fun update(entity: String) = "kotoed.db.$entity.update"
    }
}
