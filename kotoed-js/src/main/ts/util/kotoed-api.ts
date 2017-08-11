export namespace Kotoed {

    export let Address = {
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
                Error: "kotoed.api.project.error"
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
            }
        }
    }
}
