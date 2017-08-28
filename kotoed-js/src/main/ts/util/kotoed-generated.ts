/* to regenerate this file, run AddressExporter.kt */
export namespace Generated {

/*  see org/jetbrains/research/kotoed/eventbus/Address.kt */
    export const Address = {
        Api: {
            create: (entity: String) => {
                `kotoed.api.${entity}.create`
            },
            read: (entity: String) => {
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
                Create: "kotoed.api.submission.create",
                Error: "kotoed.api.submission.remoteError",
                Last: "kotoed.api.submission.last",
                Read: "kotoed.api.submission.read",
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
            ById: "/views/comment/id/:id",
            Search: "/views/comment/search",
        },
        Course: {
            Index: "/course/:id",
        },
        Project: {
            Search: "/views/project/search",
        },
        Submission: {
            Results: "/views/submission/:id/results",
        },
    }

}
