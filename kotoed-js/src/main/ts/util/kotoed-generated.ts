/* to regenerate this file, run AddressExporter.kt */
export namespace Generated {

/*  see org/jetbrains/research/kotoed/eventbus/Address.kt */
    export const Address = {
        Api: {
            create: (entity: string) => {
                return `kotoed.api.${entity}.create`;
            },
            read: (entity: string) => {
                return `kotoed.api.${entity}.read`;
            },
            Report: "kotoed.api.report",
            BuildTemplate: {
                Create: "kotoed.api.buildTemplate.create",
                Read: "kotoed.api.buildTemplate.read",
                Update: "kotoed.api.buildTemplate.update",
            },
            CommentTemplate: {
                Create: "kotoed.api.comment_template.create",
                Delete: "kotoed.api.comment_template.delete",
                ReadAll: "kotoed.api.comment_template.read_all",
                Search: "kotoed.api.comment_template.search",
                SearchCount: "kotoed.api.comment_template.search_count",
                Update: "kotoed.api.comment_template.update",
            },
            Course: {
                Create: "kotoed.api.course.create",
                Error: "kotoed.api.course.remoteError",
                Read: "kotoed.api.course.read",
                Search: "kotoed.api.course.search",
                SearchCount: "kotoed.api.course.search.count",
                Update: "kotoed.api.course.update",
                Code: {
                    List: "kotoed.api.course.code.list",
                    Read: "kotoed.api.course.code.read",
                },
                Verification: {
                    Data: "kotoed.api.course.verification.data",
                },
            },
            Denizen: {
                Create: "kotoed.api.denizen.create",
                Read: "kotoed.api.denizen.read",
                Search: "kotoed.api.denizen.search",
                SearchCount: "kotoed.api.denizen.search.count",
                Profile: {
                    Read: "kotoed.api.denizen.profile.read",
                    Update: "kotoed.api.denizen.profile.update",
                    UpdatePassword: "kotoed.api.denizen.profile.updatepassword",
                },
            },
            Notification: {
                pushRendered: (id: string) => {
                    return `kotoed.api.notification.push.${id}.render`;
                },
                Create: "kotoed.api.notification.create",
                Current: "kotoed.api.notification.current",
                MarkAllRead: "kotoed.api.notification.markRead.all",
                MarkRead: "kotoed.api.notification.markRead",
                PushRenderedBroadcast: "kotoed.api.notification.push.broadcast.render",
                RenderCurrent: "kotoed.api.notification.current.render",
            },
            OAuthProvider: {
                List: "kotoed.api.oAuthProvider.list",
            },
            Project: {
                Create: "kotoed.api.project.create",
                Delete: "kotoed.api.project.delete",
                Error: "kotoed.api.project.remoteError",
                Read: "kotoed.api.project.read",
                Search: "kotoed.api.project.search",
                SearchCount: "kotoed.api.project.search.count",
                SearchForCourse: "kotoed.api.project.searchForCourse",
                SearchForCourseCount: "kotoed.api.project.searchForCourse.count",
                Verification: {
                    Data: "kotoed.api.project.verification.data",
                },
            },
            Submission: {
                Annotations: "kotoed.api.submission.annotations",
                CommentAggregates: "kotoed.api.submission.commentAggregates",
                Comments: "kotoed.api.submission.comments",
                CommentsTotal: "kotoed.api.submission.commentsTotal",
                Create: "kotoed.api.submission.create",
                Error: "kotoed.api.submission.remoteError",
                HasOpen: "kotoed.api.submission.hasOpen",
                History: "kotoed.api.submission.history",
                Last: "kotoed.api.submission.last",
                List: "kotoed.api.submission.list",
                ListCount: "kotoed.api.submission.list.count",
                Read: "kotoed.api.submission.read",
                Update: "kotoed.api.submission.update",
                Code: {
                    Date: "kotoed.api.submission.code.date",
                    Download: "kotoed.api.submission.code.download",
                    List: "kotoed.api.submission.code.list",
                    Read: "kotoed.api.submission.code.read",
                },
                Comment: {
                    Create: "kotoed.api.submission.comment.create",
                    Read: "kotoed.api.submission.comment.read",
                    Search: "kotoed.api.submission.comment.search",
                    SearchCount: "kotoed.api.submission.comment.search.count",
                    Update: "kotoed.api.submission.comment.update",
                },
                Result: {
                    BatchRead: "kotoed.api.submission.result.read.batch",
                    Read: "kotoed.api.submission.result.read",
                },
                Tags: {
                    Create: "kotoed.api.submission.tags.create",
                    Delete: "kotoed.api.submission.tags.delete",
                    Read: "kotoed.api.submission.tags.read",
                    Search: "kotoed.api.submission.tags.search",
                    SearchCount: "kotoed.api.submission.tags.search.count",
                    Update: "kotoed.api.submission.tags.update",
                },
                Verification: {
                    Clean: "kotoed.api.submission.verification.clean",
                    Data: "kotoed.api.submission.verification.data",
                },
            },
            Tag: {
                Create: "kotoed.api.tag.create",
                Delete: "kotoed.api.tag.delete",
                List: "kotoed.api.tag.list",
                Read: "kotoed.api.tag.read",
            },
        }
    };

/*  see org/jetbrains/research/kotoed/web/UrlPattern.kt */
    export const UrlPattern = {
        EventBus: "/eventbus/*",
        Index: "/",
        Star: "/*",
        Static: "/static/*",
        Auth: {
            DoLogin: "/auth/login/doLogin",
            DoSignUp: "/auth/login/doSignUp",
            Index: "/auth/login",
            LoginDone: "/auth/login/done",
            Logout: "/auth/logout",
            OAuthCallback: "/auth/oauth/callback/:providerName",
            OAuthStart: "/auth/oauth/start/:providerName",
            ResetPassword: "/auth/resetPassword",
            RestorePassword: "/auth/restorePassword/:uid",
        },
        AuthHelpers: {
            CoursePerms: "/auth/perms/course/:id",
            ProjectPerms: "/auth/perms/project/:id",
            RootPerms: "/auth/perms/root",
            SubmissionPerms: "/auth/perms/submission/:id",
            WhoAmI: "/auth/whoAmI",
        },
        BuildTemplate: {
            Edit: "/buildTemplate/edit/:id",
        },
        CodeReview: {
            Index: "/submission/:id/review/*",
        },
        Comment: {
            ById: "/redirect/comment/:id",
            Search: "/search/comment",
        },
        CommentTemplate: {
            Show: "/commentTemplates",
        },
        Course: {
            Edit: "/course/edit/:id",
            Index: "/course/:id",
        },
        Denizen: {
            Search: "/search/denizen",
        },
        Profile: {
            Edit: "/auth/profile/edit/:id",
            Index: "/auth/profile/:id",
        },
        Project: {
            Index: "/project/:id",
            Search: "/search/project",
        },
        Redirect: {
            ById: "/redirect/:entity/:id",
        },
        Submission: {
            Index: "/submission/:id",
            NotificationRedirect: "/redirect/submission/:id",
            Results: "/submission/:id/results",
            SearchByTags: "/search/byTags",
        },
        SubmissionResults: {
            ById: "/redirect/submissionResults/:id",
        },
    }

}
