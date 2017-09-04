package org.jetbrains.research.kotoed.eventbus

object Address {
    object Api {
        fun create(entity: String) = "kotoed.api.$entity.create"
        fun read(entity: String) = "kotoed.api.$entity.read"

        object Course {
            const val Create = "kotoed.api.course.create"
            const val Read = "kotoed.api.course.read"
            const val Error = "kotoed.api.course.remoteError"
            const val Search = "kotoed.api.course.search"
            const val SearchCount = "kotoed.api.course.search.count"

            object Verification {
                const val Data = "kotoed.api.course.verification.data"
            }
        }

        object Project {
            const val Create = "kotoed.api.project.create"
            const val Read = "kotoed.api.project.read"
            const val Error = "kotoed.api.project.remoteError"
            const val Search = "kotoed.api.project.search"
            const val SearchCount = "kotoed.api.project.search.count"
            const val SearchForCourse = "kotoed.api.project.searchForCourse"
            const val SearchForCourseCount = "kotoed.api.project.searchForCourse.count"


            object Verification {
                const val Data = "kotoed.api.project.verification.data"
            }
        }

        object Tag {
            const val Create = "kotoed.api.tag.create"
            const val Read = "kotoed.api.tag.read"
            const val Delete = "kotoed.api.tag.delete"

            const val List = "kotoed.api.tag.list"
        }

        object Submission {
            const val Read = "kotoed.api.submission.read"
            const val Last = "kotoed.api.submission.last"
            const val Create = "kotoed.api.submission.create"
            const val Update = "kotoed.api.submission.update"
            const val Comments = "kotoed.api.submission.comments"
            const val CommentAggregates = "kotoed.api.submission.commentAggregates"
            const val CommentsTotal = "kotoed.api.submission.commentsTotal"
            const val Error = "kotoed.api.submission.remoteError"
            const val List = "kotoed.api.submission.list"
            const val ListCount = "kotoed.api.submission.list.count"
            const val History = "kotoed.api.submission.history"

            object Tags {
                const val Create = "kotoed.api.submission.tags.create"
                const val Read = "kotoed.api.submission.tags.read"
                const val Update = "kotoed.api.submission.tags.update"
                const val Delete = "kotoed.api.submission.tags.delete"

                const val Search = "kotoed.api.submission.tags.search"
                const val SearchCount = "kotoed.api.submission.tags.search.count"
            }

            object Verification {
                const val Data = "kotoed.api.submission.verification.data"
                const val Clean = "kotoed.api.submission.verification.clean"
            }

            object Comment {
                const val Read = "kotoed.api.submission.comment.read"
                const val Update = "kotoed.api.submission.comment.update"
                const val Create = "kotoed.api.submission.comment.create"
                const val Search = "kotoed.api.submission.comment.search"
                const val SearchCount = "kotoed.api.submission.comment.search.count"
            }

            object Code {
                const val Download = "kotoed.api.submission.code.download"
                const val Read = "kotoed.api.submission.code.read"
                const val List = "kotoed.api.submission.code.list"
            }

            object Result {
                const val Read = "kotoed.api.submission.result.read"
            }
        }

        object Denizen {
            const val Create = "kotoed.api.denizen.create"
            const val Read = "kotoed.api.denizen.read"

            object Profile {
                const val Read = "kotoed.api.denizen.profile.read"
                const val Update = "kotoed.api.denizen.profile.update"
            }
        }

        object Notification {
            const val Current = "kotoed.api.notification.current"
            const val RenderCurrent = "kotoed.api.notification.current.render"
            const val MarkRead = "kotoed.api.notification.markRead"
            const val Create = "kotoed.api.notification.create"

            fun pushRendered(id: String) = "kotoed.api.notification.push.$id.render"
        }

        object OAuthProvider {
            const val List = "kotoed.api.oAuthProvider.list"
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

            const val SetPassword = "kotoed.user.auth.setPassword"

            const val Restore = "kotoed.user.auth.restore"
            const val RestoreSecret = "kotoed.user.auth.restore.secret"
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
        fun query(entity: String) = "kotoed.db.$entity.query"
        fun count(entity: String) = "kotoed.db.$entity.count"

        fun find(entity: String) = "kotoed.db.$entity.find"
        fun readFor(entity: String, key: String) = "${read(entity)}.for.$key"
        fun update(entity: String) = "kotoed.db.$entity.update"

        fun searchText(entity: String) = "kotoed.db.$entity.search.text"

        // special addresses only for some props
        fun full(entity: String) = "kotoed.db.$entity.read.full"

        fun last(entity: String) = "kotoed.db.$entity.read.last"

        fun process(entity: String) = "kotoed.db.$entity.process"
        fun verify(entity: String) = "kotoed.db.$entity.verify"
        fun clean(entity: String) = "kotoed.db.$entity.clean"

        object Submission {
            object Tags {
                const val Update = "kotoed.db.submission.tags.update"
            }
        }
    }

    object Notifications {
        object Email {
            const val Send = "kotoed.notifications.email.send"
        }
    }

    const val Schedule = "kotoed.schedule"

}
