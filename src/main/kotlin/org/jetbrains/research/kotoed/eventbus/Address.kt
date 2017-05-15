package org.jetbrains.research.kotoed.eventbus

object Address {
    object Api {
        fun create(entity: String) = "kotoed.api.$entity.create"
        fun read(entity: String) = "kotoed.api.$entity.read"

        object Course {
            const val Create = "kotoed.api.course.create"
            const val Read = "kotoed.api.course.read"
            const val Error = "kotoed.api.course.error"
        }

        object Project {
            const val Create = "kotoed.api.project.create"
            const val Read = "kotoed.api.project.read"
            const val Error = "kotoed.api.project.error"
        }

        object Submission {
            const val Read = "kotoed.api.submission.read"
            const val Last = "kotoed.api.submission.last"
            const val Create = "kotoed.api.submission.create"
            const val Comments = "kotoed.api.submission.comments"
            const val Error = "kotoed.api.submission.error"

            object Comment {
                const val Read = "kotoed.api.submission.comment.read"
                const val Create = "kotoed.api.submission.comment.create"
            }

        }

        object Denizen {
            const val Create = "kotoed.api.denizen.create"
            const val Read = "kotoed.api.denizen.read"
        }
    }

    object TeamCity {
        const val Proxy = "kotoed.teamcity.proxy"

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
        fun find(entity: String) = "kotoed.db.$entity.find"
        fun readFor(entity: String, key: String) = "${read(entity)}.for.$key"
        fun update(entity: String) = "kotoed.db.$entity.update"

        fun process(entity: String) = "kotoed.db.$entity.process"
        fun verify(entity: String) = "kotoed.db.$entity.verify"
    }

}
