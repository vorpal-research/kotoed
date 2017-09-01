/* to regenerate this file, run AddressExporter.kt */
export namespace Generated {

/*  see org/jetbrains/research/kotoed/eventbus/Address.kt */
    export const Address = {
        Api: {
            create: (entity: string) => {
                `kotoed.api.${entity}.create`
            },
            read: (entity: string) => {
                `kotoed.api.${entity}.read`
            },
            Course: {
                Create: "kotoed.api.course.create",
                Error: "kotoed.api.course.remoteError",
                Read: "kotoed.api.course.read",
                Search: "kotoed.api.course.search",
                SearchCount: "kotoed.api.course.search.count",
                Verification: {
                    Data: "kotoed.api.course.verification.data",
                },
            },
            Denizen: {
                Create: "kotoed.api.denizen.create",
                Read: "kotoed.api.denizen.read",
            },
            Notification: {
                Current: "kotoed.api.notification.current",
                MarkRead: "kotoed.api.notification.markRead",
                RenderCurrent: "kotoed.api.notification.current.render",
            },
            OAuthProvider: {
                List: "kotoed.api.oAuthProvider.list",
            },
            Project: {
                Create: "kotoed.api.project.create",
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
                CommentAggregates: "kotoed.api.submission.commentAggregates",
                Comments: "kotoed.api.submission.comments",
                CommentsTotal: "kotoed.api.submission.commentsTotal",
                Create: "kotoed.api.submission.create",
                Error: "kotoed.api.submission.remoteError",
                History: "kotoed.api.submission.history",
                Last: "kotoed.api.submission.last",
                List: "kotoed.api.submission.list",
                ListCount: "kotoed.api.submission.list.count",
                Read: "kotoed.api.submission.read",
                Update: "kotoed.api.submission.update",
                Code: {
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
                    Read: "kotoed.api.submission.result.read",
                },
                Verification: {
                    Clean: "kotoed.api.submission.verification.clean",
                    Data: "kotoed.api.submission.verification.data",
                },
            },
        }
    };

/*  see org/jetbrains/research/kotoed/web/UrlPattern.kt */
    export const UrlPattern = {
        EventBus: "/eventbus/*",
        Index: "/",
        NotImplemented: "/notImplemented",
        Star: "/*",
        Static: "/static/*",
        Auth: {
            DoLogin: "/login/doLogin",
            DoSignUp: "/login/doSignUp",
            Index: "/login",
            LoginDone: "/login/done",
            Logout: "/logout",
            OAuthCallback: "/login/oauth/callback/:providerName",
            OAuthStart: "/login/oauth/start/:providerName",
            ResetPassword: "/resetPassword",
            RestorePassword: "/restorePassword/:uid",
        },
        AuthHelpers: {
            CoursePerms: "/perms/course/:id",
            ProjectPerms: "/perms/project/:id",
            RootPerms: "/perms/root",
            SubmissionPerms: "/perms/submission/:id",
            WhoAmI: "/whoAmI",
        },
        CodeReview: {
            Index: "/codereview/:id/*",
        },
        Comment: {
            ById: "/redirect/comment/:id",
            Search: "/views/comment/search",
        },
        Course: {
            Index: "/course/:id",
        },
        Project: {
            Index: "/project/:id",
            Search: "/views/project/search",
        },
        Redirect: {
            ById: "/redirect/:entity/:id",
        },
        Submission: {
            Index: "/submission/:id",
            Results: "/views/submission/:id/results",
        },
        SubmissionResults: {
            ById: "/redirect/submissionResults/:id",
        },
    }

}
