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
                HasOpen: "kotoed.api.submission.hasOpen",
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
        Profile: {
            Edit: "/profile/edit/:id",
            Index: "/profile/:id",
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
            NotificationRedirect: "/redirect/submission/:id",
            Results: "/views/submission/:id/results",
            SearchByTags: "/views/submission/searchByTags",
        },
        SubmissionResults: {
            ById: "/redirect/submissionResults/:id",
        },
    }

}
