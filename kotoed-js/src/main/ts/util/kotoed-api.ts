export namespace Kotoed {

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
                Read: "kotoed.api.course.read",
                Error: "kotoed.api.course.error"
            },

            Project: {
                Create: "kotoed.api.project.create",
                Read: "kotoed.api.project.read",
                Error: "kotoed.api.project.error",
                Search: "kotoed.api.project.search",
                SearchCount: "kotoed.api.project.search.count"
            },

            Submission: {
                Read: "kotoed.api.submission.read",
                Last: "kotoed.api.submission.last",
                Create: "kotoed.api.submission.create",
                Comments: "kotoed.api.submission.comments",
                CommentAggregates: "kotoed.api.submission.commentAggregates",
                Error: "kotoed.api.submission.error",

                Comment: {
                    Read: "kotoed.api.submission.comment.read",
                    Update: "kotoed.api.submission.comment.update",
                    Create: "kotoed.api.submission.comment.create",
                    Search: "kotoed.api.submission.comment.search",
                    SearchCount: "kotoed.api.submission.comment.search.count"
                },

                Code: {
                    Download: "kotoed.api.submission.code.download",
                    Read: "kotoed.api.submission.code.read",
                    List: "kotoed.api.submission.code.list"
                },

                Result: {
                    Read: "kotoed.api.submission.result.read"
                }
            },

            Denizen: {
                Create: "kotoed.api.denizen.create",
                Read: "kotoed.api.denizen.read"
            },

            Notification: {
                Create: "kotoed.api.notification.create",
                Current: "kotoed.api.notification.current",
                MarkRead: "kotoed.api.notification.markRead"
            },
            OAuthProvider: {
                List: "kotoed.api.oAuthProvider.list"

            }
        }
    };

    export const UrlPattern = {
        Index: "/",

        Star: "/*",

        CodeReview: {
            Index: "/codereview/:id/*",
            Capabilities: "/codereview-api/caps/:id"
        },

        Auth: {
            Index: "/login",
            DoLogin: "/login/doLogin",
            DoSignUp: "/login/doSignUp",
            LoginDone: "/login/done",
            Logout: "/logout",
            OAuthStart: "/login/oauth/start/:providerName",
            OAuthCallback: "/login/oauth/callback/:providerName"
        },

        Submission: {
            Results: "/views/submission/:id/results"
        },

        EventBus: "/eventbus/*",
        Static: "/static/*",

        reverse(pattern: string, params: {[name: string]: string|number}, star: string|number = ""): string {
            let url = pattern;
            for (let k in params) {
                url = url.replace(`:${k}`, `${params[k]}`)
            }

            url = url.replace("*", `${star}`);

            return url
        }
    }
}
