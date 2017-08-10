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
            const val CommentAggregates = "kotoed.api.submission.commentAggregates"
            const val Error = "kotoed.api.submission.error"

            object Comment {
                const val Read = "kotoed.api.submission.comment.read"
                const val Update = "kotoed.api.submission.comment.update"
                const val Create = "kotoed.api.submission.comment.create"

            }

            object Code {
                const val Download = "kotoed.api.submission.code.download"
                const val Read = "kotoed.api.submission.code.read"
                const val List = "kotoed.api.submission.code.list"
            }

        }

        object Denizen {
            const val Create = "kotoed.api.denizen.create"
            const val Read = "kotoed.api.denizen.read"
        }

        object Notification {
            const val Create = "kotoed.api.notification.create"
            const val Current = "kotoed.api.notification.current"
            const val MarkRead = "kotoed.api.notification.markRead"
        }
    }

    object Buildbot {
        object Project {
            const val Create = "kotoed.buildbot.project.create"
        }

        object Build {
            const val Trigger = "kotoed.buildbot.build.trigger"
            const val RequestInfo = "kotoed.buildbot.build.requestinfo"
            const val BuildCrawl = "kotoed.buildbot.build.buildcrawl"
            const val StepCrawl = "kotoed.buildbot.build.stepcrawl"
            const val LogCrawl = "kotoed.buildbot.build.logcrawl"
            const val LogContent = "kotoed.buildbot.build.logcontent"
        }
    }

    object Code {
        const val Ping = "kotoed.code.ping"
        const val Download = "kotoed.code.download"
        const val Read = "kotoed.code.read"
        const val List = "kotoed.code.list"
        const val Diff = "kotoed.code.diff"
        const val Info = "kotoed.code.info"
        const val LocationDiff = "kotoed.code.diff.location"
        const val PurgeCache = "kotoed.code.purgecache"

        const val KloneCheck = "kotoed.code.klonecheck"
    }

    object User {
        object Auth {
            const val SignUp = "kotoed.user.auth.signup"
            const val Login = "kotoed.user.auth.login"
            const val Info = "kotoed.user.auth.info"
            const val HasPerm = "kotoed.user.auth.hasPerm"
        }

        object OAuth {
            const val SignUp = "kotoed.user.oauth.signup"
            const val Login = "kotoed.user.oauth.login"
        }
    }

    object DB {
        fun create(entity: String) = "kotoed.db.$entity.create"
        fun delete(entity: String) = "kotoed.db.$entity.delete"
        fun read(entity: String) = "kotoed.db.$entity.read"
        fun find(entity: String) = "kotoed.db.$entity.find"
        fun readFor(entity: String, key: String) = "${read(entity)}.for.$key"
        fun update(entity: String) = "kotoed.db.$entity.update"

        // special addresses only for some props
        fun full(entity: String) = "kotoed.db.$entity.read.full"
        fun last(entity: String) = "kotoed.db.$entity.read.last"

        fun process(entity: String) = "kotoed.db.$entity.process"
        fun verify(entity: String) = "kotoed.db.$entity.verify"
    }

    object Notifications {
        object Email {
            const val Send = "kotoed.notifications.email.send"
        }
    }

    const val Schedule = "kotoed.schedule"

}
