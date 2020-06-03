import {Mapping} from './types'
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
            BuildSystem: {
                Build: {
                    Status: "kotoed.api.build.status" as "kotoed.api.build.status",
                    Summary: "kotoed.api.build.summary" as "kotoed.api.build.summary",
                },
            },
            BuildTemplate: {
                Create: "kotoed.api.buildTemplate.create" as "kotoed.api.buildTemplate.create",
                Read: "kotoed.api.buildTemplate.read" as "kotoed.api.buildTemplate.read",
                Update: "kotoed.api.buildTemplate.update" as "kotoed.api.buildTemplate.update",
            },
            CommentTemplate: {
                Create: "kotoed.api.comment_template.create" as "kotoed.api.comment_template.create",
                Delete: "kotoed.api.comment_template.delete" as "kotoed.api.comment_template.delete",
                ReadAll: "kotoed.api.comment_template.read_all" as "kotoed.api.comment_template.read_all",
                Search: "kotoed.api.comment_template.search" as "kotoed.api.comment_template.search",
                SearchCount: "kotoed.api.comment_template.search_count" as "kotoed.api.comment_template.search_count",
                Update: "kotoed.api.comment_template.update" as "kotoed.api.comment_template.update",
            },
            Course: {
                Create: "kotoed.api.course.create" as "kotoed.api.course.create",
                Error: "kotoed.api.course.remoteError" as "kotoed.api.course.remoteError",
                Read: "kotoed.api.course.read" as "kotoed.api.course.read",
                Report: "kotoed.api.course.report" as "kotoed.api.course.report",
                Search: "kotoed.api.course.search" as "kotoed.api.course.search",
                SearchCount: "kotoed.api.course.search.count" as "kotoed.api.course.search.count",
                Update: "kotoed.api.course.update" as "kotoed.api.course.update",
                Code: {
                    List: "kotoed.api.course.code.list" as "kotoed.api.course.code.list",
                    Read: "kotoed.api.course.code.read" as "kotoed.api.course.code.read",
                },
                Verification: {
                    Data: "kotoed.api.course.verification.data" as "kotoed.api.course.verification.data",
                },
            },
            Denizen: {
                Create: "kotoed.api.denizen.create" as "kotoed.api.denizen.create",
                Read: "kotoed.api.denizen.read" as "kotoed.api.denizen.read",
                Search: "kotoed.api.denizen.search" as "kotoed.api.denizen.search",
                SearchCount: "kotoed.api.denizen.search.count" as "kotoed.api.denizen.search.count",
                Profile: {
                    Read: "kotoed.api.denizen.profile.read" as "kotoed.api.denizen.profile.read",
                    Update: "kotoed.api.denizen.profile.update" as "kotoed.api.denizen.profile.update",
                    UpdatePassword: "kotoed.api.denizen.profile.updatepassword" as "kotoed.api.denizen.profile.updatepassword",
                },
            },
            Notification: {
                pushRendered: (id: string) => {
                    return `kotoed.api.notification.push.${id}.render`;
                },
                Create: "kotoed.api.notification.create" as "kotoed.api.notification.create",
                Current: "kotoed.api.notification.current" as "kotoed.api.notification.current",
                MarkAllRead: "kotoed.api.notification.markRead.all" as "kotoed.api.notification.markRead.all",
                MarkRead: "kotoed.api.notification.markRead" as "kotoed.api.notification.markRead",
                PushRenderedBroadcast: "kotoed.api.notification.push.broadcast.render" as "kotoed.api.notification.push.broadcast.render",
                Read: "kotoed.api.notification.read" as "kotoed.api.notification.read",
                Render: "kotoed.api.notification.render" as "kotoed.api.notification.render",
                RenderCurrent: "kotoed.api.notification.current.render" as "kotoed.api.notification.current.render",
                Web: {
                    PublicKey: "kotoed.api.notification.web.publicKey" as "kotoed.api.notification.web.publicKey",
                    Subscribe: "kotoed.api.notification.web.subscribe" as "kotoed.api.notification.web.subscribe",
                },
            },
            OAuthProvider: {
                List: "kotoed.api.oAuthProvider.list" as "kotoed.api.oAuthProvider.list",
            },
            Project: {
                Create: "kotoed.api.project.create" as "kotoed.api.project.create",
                Delete: "kotoed.api.project.delete" as "kotoed.api.project.delete",
                Error: "kotoed.api.project.remoteError" as "kotoed.api.project.remoteError",
                Read: "kotoed.api.project.read" as "kotoed.api.project.read",
                Search: "kotoed.api.project.search" as "kotoed.api.project.search",
                SearchCount: "kotoed.api.project.search.count" as "kotoed.api.project.search.count",
                SearchForCourse: "kotoed.api.project.searchForCourse" as "kotoed.api.project.searchForCourse",
                SearchForCourseCount: "kotoed.api.project.searchForCourse.count" as "kotoed.api.project.searchForCourse.count",
                Verification: {
                    Data: "kotoed.api.project.verification.data" as "kotoed.api.project.verification.data",
                },
            },
            Submission: {
                Annotations: "kotoed.api.submission.annotations" as "kotoed.api.submission.annotations",
                CommentAggregates: "kotoed.api.submission.commentAggregates" as "kotoed.api.submission.commentAggregates",
                Comments: "kotoed.api.submission.comments" as "kotoed.api.submission.comments",
                CommentsTotal: "kotoed.api.submission.commentsTotal" as "kotoed.api.submission.commentsTotal",
                Create: "kotoed.api.submission.create" as "kotoed.api.submission.create",
                Error: "kotoed.api.submission.remoteError" as "kotoed.api.submission.remoteError",
                HasOpen: "kotoed.api.submission.hasOpen" as "kotoed.api.submission.hasOpen",
                History: "kotoed.api.submission.history" as "kotoed.api.submission.history",
                Last: "kotoed.api.submission.last" as "kotoed.api.submission.last",
                List: "kotoed.api.submission.list" as "kotoed.api.submission.list",
                ListCount: "kotoed.api.submission.list.count" as "kotoed.api.submission.list.count",
                Read: "kotoed.api.submission.read" as "kotoed.api.submission.read",
                Report: "kotoed.api.submission.report" as "kotoed.api.submission.report",
                Update: "kotoed.api.submission.update" as "kotoed.api.submission.update",
                Code: {
                    Date: "kotoed.api.submission.code.date" as "kotoed.api.submission.code.date",
                    Download: "kotoed.api.submission.code.download" as "kotoed.api.submission.code.download",
                    List: "kotoed.api.submission.code.list" as "kotoed.api.submission.code.list",
                    Read: "kotoed.api.submission.code.read" as "kotoed.api.submission.code.read",
                },
                Comment: {
                    Create: "kotoed.api.submission.comment.create" as "kotoed.api.submission.comment.create",
                    Read: "kotoed.api.submission.comment.read" as "kotoed.api.submission.comment.read",
                    Search: "kotoed.api.submission.comment.search" as "kotoed.api.submission.comment.search",
                    SearchCount: "kotoed.api.submission.comment.search.count" as "kotoed.api.submission.comment.search.count",
                    Update: "kotoed.api.submission.comment.update" as "kotoed.api.submission.comment.update",
                },
                Result: {
                    BatchRead: "kotoed.api.submission.result.read.batch" as "kotoed.api.submission.result.read.batch",
                    Read: "kotoed.api.submission.result.read" as "kotoed.api.submission.result.read",
                    Report: "kotoed.api.submission.result.report" as "kotoed.api.submission.result.report",
                },
                Tags: {
                    Create: "kotoed.api.submission.tags.create" as "kotoed.api.submission.tags.create",
                    Delete: "kotoed.api.submission.tags.delete" as "kotoed.api.submission.tags.delete",
                    Read: "kotoed.api.submission.tags.read" as "kotoed.api.submission.tags.read",
                    Search: "kotoed.api.submission.tags.search" as "kotoed.api.submission.tags.search",
                    SearchCount: "kotoed.api.submission.tags.search.count" as "kotoed.api.submission.tags.search.count",
                    Update: "kotoed.api.submission.tags.update" as "kotoed.api.submission.tags.update",
                },
                Verification: {
                    Clean: "kotoed.api.submission.verification.clean" as "kotoed.api.submission.verification.clean",
                    Data: "kotoed.api.submission.verification.data" as "kotoed.api.submission.verification.data",
                },
            },
            Tag: {
                Create: "kotoed.api.tag.create" as "kotoed.api.tag.create",
                Delete: "kotoed.api.tag.delete" as "kotoed.api.tag.delete",
                List: "kotoed.api.tag.list" as "kotoed.api.tag.list",
                Read: "kotoed.api.tag.read" as "kotoed.api.tag.read",
            },
        }
    };
    /*  see org/jetbrains/research/kotoed/web/UrlPattern.kt */
    export const UrlPattern = {
        EventBus: "/eventbus/*" as "/eventbus/*",
        Index: "/" as "/",
        Star: "/*" as "/*",
        Static: "/static/*" as "/static/*",
        Auth: {
            DoLogin: "/auth/login/doLogin" as "/auth/login/doLogin",
            DoSignUp: "/auth/login/doSignUp" as "/auth/login/doSignUp",
            Index: "/auth/login" as "/auth/login",
            LoginDone: "/auth/login/done" as "/auth/login/done",
            Logout: "/auth/logout" as "/auth/logout",
            OAuthCallback: "/auth/oauth/callback/:providerName" as "/auth/oauth/callback/:providerName",
            OAuthStart: "/auth/oauth/start/:providerName" as "/auth/oauth/start/:providerName",
            ResetPassword: "/auth/resetPassword" as "/auth/resetPassword",
            RestorePassword: "/auth/restorePassword/:uid" as "/auth/restorePassword/:uid",
        },
        AuthHelpers: {
            CoursePerms: "/auth/perms/course/:id" as "/auth/perms/course/:id",
            ProjectPerms: "/auth/perms/project/:id" as "/auth/perms/project/:id",
            RootPerms: "/auth/perms/root" as "/auth/perms/root",
            SubmissionPerms: "/auth/perms/submission/:id" as "/auth/perms/submission/:id",
            WhoAmI: "/auth/whoAmI" as "/auth/whoAmI",
        },
        BuildSystem: {
            Status: "/build/:id" as "/build/:id",
            Summary: "/builds" as "/builds",
        },
        BuildTemplate: {
            Edit: "/buildTemplate/edit/:id" as "/buildTemplate/edit/:id",
        },
        CodeReview: {
            Index: "/submission/:id/review/*" as "/submission/:id/review/*",
        },
        Comment: {
            ById: "/redirect/comment/:id" as "/redirect/comment/:id",
            Search: "/search/comment" as "/search/comment",
        },
        CommentTemplate: {
            Show: "/commentTemplates" as "/commentTemplates",
        },
        Course: {
            Edit: "/course/edit/:id" as "/course/edit/:id",
            Index: "/course/:id" as "/course/:id",
            Report: "/course/report/:id" as "/course/report/:id",
        },
        Denizen: {
            Search: "/search/denizen" as "/search/denizen",
        },
        Notification: {
            ById: "/notification/:id" as "/notification/:id",
        },
        Profile: {
            Edit: "/auth/profile/edit/:id" as "/auth/profile/edit/:id",
            Index: "/auth/profile/:id" as "/auth/profile/:id",
        },
        Project: {
            Index: "/project/:id" as "/project/:id",
            Search: "/search/project" as "/search/project",
        },
        Redirect: {
            ById: "/redirect/:entity/:id" as "/redirect/:entity/:id",
            Root: "/redirect/" as "/redirect/",
        },
        Submission: {
            Index: "/submission/:id" as "/submission/:id",
            NotificationRedirect: "/redirect/submission/:id" as "/redirect/submission/:id",
            Results: "/submission/:id/results" as "/submission/:id/results",
            SearchByTags: "/search/byTags" as "/search/byTags",
        },
        SubmissionResults: {
            ById: "/redirect/submissionResults/:id" as "/redirect/submissionResults/:id",
        },
    };
    /*  see server code */
    export interface ApiBindingOutputs {
        ["kotoed.api.submission.result.report"]: {data: Array<Array<string>>};
        ["kotoed.api.project.create"]: {record: {courseId: number, denizenId: number, repoType: string, repoUrl: string, deleted: boolean, name: string, id: number}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.submission.tags.update"]: any;
        ["kotoed.api.notification.create"]: {denizenId: number, time?: {nanos: number}, status: 'unread' | 'read', body?: any, title: string, id: number, type: string};
        ["kotoed.api.denizen.create"]: {record: {denizenId?: string, email?: string, id?: number}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.submission.create"]: {record: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId: number, id: number, state: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.submission.commentAggregates"]: {byFile: Array<{aggregate: Mapping<'open' | 'closed', number>, file: string}>, lost: Mapping<'open' | 'closed', number>};
        ["kotoed.api.submission.history"]: Array<any>;
        ["kotoed.api.course.report"]: {data: Array<Array<string>>};
        ["kotoed.api.submission.comment.update"]: {record: {authorId: number, datetime?: number, sourcefile: string, sourceline: number, submissionId: number, originalSubmissionId: number, persistentCommentId: number, previousCommentId?: number, text: string, id: number, state: 'open' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.submission.tags.search"]: Array<any>;
        ["kotoed.api.comment_template.read_all"]: Array<{denizenId: number, text: string, name: string, id: number}>;
        ["kotoed.api.submission.result.read.batch"]: Array<any>;
        ["kotoed.api.denizen.profile.updatepassword"]: void;
        ["kotoed.api.buildTemplate.read"]: {commandLine: Array<{commandLine: Array<string>, type: 'SHELL'}>, environment: Array<{name: string, value: string}>, id: number};
        ["kotoed.api.oAuthProvider.list"]: Array<any>;
        ["kotoed.api.denizen.profile.update"]: void;
        ["kotoed.api.submission.commentsTotal"]: Mapping<'open' | 'closed', number>;
        ["kotoed.api.submission.code.download"]: {errors: Array<string>, status: 'done' | 'failed' | 'pending', uid: string, url: string, vcs?: 'git' | 'mercurial'};
        ["kotoed.api.denizen.search.count"]: {count: number};
        ["kotoed.api.submission.report"]: {data: Array<Array<string>>};
        ["kotoed.api.submission.result.read"]: {records: Array<any>, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.project.read"]: {record: {courseId: number, denizenId: number, repoType: string, repoUrl: string, deleted: boolean, name: string, id: number}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.notification.markRead"]: {denizenId: number, time?: {nanos: number}, status: 'unread' | 'read', body?: any, title: string, id: number, type: string};
        ["kotoed.api.denizen.search"]: Array<any>;
        ["kotoed.api.denizen.read"]: {record: {denizenId?: string, email?: string, id?: number}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.tag.delete"]: {style: any, name: string, id: number};
        ["kotoed.api.submission.read"]: {record: any, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.notification.read"]: {denizenId: number, time?: {nanos: number}, status: 'unread' | 'read', body?: any, title: string, id: number, type: string};
        ["kotoed.api.submission.last"]: {record: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId: number, id: number, state: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.notification.current"]: Array<{denizenId: number, time?: {nanos: number}, status: 'unread' | 'read', body?: any, title: string, id: number, type: string}>;
        ["kotoed.api.comment_template.delete"]: {denizenId: number, text: string, name: string, id: number};
        ["kotoed.api.course.update"]: {record: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId: number, icon?: string, name: string, id: number, state: 'open' | 'frozen' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.submission.list"]: Array<any>;
        ["kotoed.api.notification.web.publicKey"]: any;
        ["kotoed.api.comment_template.update"]: {denizenId: number, text: string, name: string, id: number};
        ["kotoed.api.notification.markRead.all"]: void;
        ["kotoed.api.course.remoteError"]: Array<{courseId: number, data?: any, id: number}>;
        ["kotoed.api.notification.web.subscribe"]: {denizenId: number, subscriptionObject: any, id: number};
        ["kotoed.api.course.verification.data"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.tags.delete"]: Array<{submissionId: number, tagId: number, id: number}>;
        ["kotoed.api.notification.current.render"]: Array<{contents: string, id: number, linkTo: {entity: string, id: string}}>;
        ["kotoed.api.submission.list.count"]: {count: number};
        ["kotoed.api.build.status"]: {commands: Array<{cerr: string, commandLine: string, cout: string, state: 'RUNNING' | 'FINISHED' | 'WAITING'}>, descriptor: string, request: {buildId: number, buildScript: Array<{commandLine: Array<string>, type: 'SHELL'}>, env?: Array<[string, string]>, submissionId: number}, startTime: number};
        ["kotoed.api.buildTemplate.create"]: {commandLine: Array<{commandLine: Array<string>, type: 'SHELL'}>, environment: Array<{name: string, value: string}>, id: number};
        ["kotoed.api.tag.list"]: Array<{style: any, name: string, id: number}>;
        ["kotoed.api.course.search"]: Array<any>;
        ["kotoed.api.submission.annotations"]: {map: Array<[string, Array<{message: string, position: {col: number, line: number}, severity: 'error' | 'warning'}>]>};
        ["kotoed.api.notification.render"]: {contents: string, id: number, linkTo: {entity: string, id: string}};
        ["kotoed.api.course.read"]: {record: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId: number, icon?: string, name: string, id: number, state: 'open' | 'frozen' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.comment_template.search"]: Array<{denizenId: number, text: string, name: string, id: number}>;
        ["kotoed.api.build.summary"]: Array<{commands: Array<{cerr: string, commandLine: string, cout: string, state: 'RUNNING' | 'FINISHED' | 'WAITING'}>, descriptor: string, request: {buildId: number, buildScript: Array<{commandLine: Array<string>, type: 'SHELL'}>, env?: Array<[string, string]>, submissionId: number}, startTime: number}>;
        ["kotoed.api.submission.comments"]: {byFile: Array<{byLine: Array<{comments: Array<any>, line: number}>, filename: string}>, lost: any};
        ["kotoed.api.comment_template.search_count"]: {count: number};
        ["kotoed.api.course.search.count"]: {count: number};
        ["kotoed.api.submission.verification.data"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.tags.read"]: Array<{style: any, name: string, id: number}>;
        ["kotoed.api.submission.remoteError"]: Array<{submissionId: number, data?: any, id: number}>;
        ["kotoed.api.submission.tags.create"]: {submissionId: number, tagId: number, id: number};
        ["kotoed.api.project.verification.data"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.comment.create"]: {record: {authorId: number, datetime?: number, sourcefile: string, sourceline: number, submissionId: number, originalSubmissionId: number, persistentCommentId: number, previousCommentId?: number, text: string, id: number, state: 'open' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.project.remoteError"]: Array<{data?: any, projectId: number, id: number}>;
        ["kotoed.api.submission.tags.search.count"]: {count: number};
        ["kotoed.api.project.delete"]: void;
        ["kotoed.api.submission.update"]: {record: any, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.tag.read"]: {style: any, name: string, id: number};
        ["kotoed.api.buildTemplate.update"]: {commandLine: Array<{commandLine: Array<string>, type: 'SHELL'}>, environment: Array<{name: string, value: string}>, id: number};
        ["kotoed.api.project.searchForCourse.count"]: {count: number};
        ["kotoed.api.submission.verification.clean"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.code.date"]: {time: number};
        ["kotoed.api.comment_template.create"]: {denizenId: number, text: string, name: string, id: number};
        ["kotoed.api.course.create"]: {record: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId: number, icon?: string, name: string, id: number, state: 'open' | 'frozen' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.project.searchForCourse"]: Array<any>;
        ["kotoed.api.tag.create"]: {style: any, name: string, id: number};
        ["kotoed.api.denizen.profile.read"]: {denizenId: string, email?: string, emailNotifications: boolean, firstName?: string, group?: string, id: number, lastName?: string, oauth: Array<[string, string]>};
        ["kotoed.api.submission.comment.search.count"]: {count: number};
        ["kotoed.api.course.code.read"]: {contents: string, status: 'done' | 'failed' | 'pending'};
        ["kotoed.api.submission.code.read"]: {contents: string, status: 'done' | 'failed' | 'pending'};
        ["kotoed.api.submission.comment.search"]: Array<any>;
        ["kotoed.api.submission.code.list"]: {root?: {changed: boolean, children?: Array<any>, name: string, type: 'directory' | 'file'}, status: 'done' | 'failed' | 'pending'};
        ["kotoed.api.submission.comment.read"]: {record: {authorId: number, datetime?: number, sourcefile: string, sourceline: number, submissionId: number, originalSubmissionId: number, persistentCommentId: number, previousCommentId?: number, text: string, id: number, state: 'open' | 'closed'}, verificationData: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'}};
        ["kotoed.api.course.code.list"]: {root?: {changed: boolean, children?: Array<any>, name: string, type: 'directory' | 'file'}, status: 'done' | 'failed' | 'pending'};
        [key: string]: any
    };
    export interface ApiBindingInputs {
        ["kotoed.api.submission.result.report"]: {id: number, timestamp?: number};
        ["kotoed.api.project.create"]: {courseId?: number, denizenId?: number, repoType?: string, repoUrl?: string, deleted?: boolean, name?: string, id?: number};
        ["kotoed.api.submission.tags.update"]: {submissionId: number, tags: Array<{style?: any, name?: string, id?: number}>};
        ["kotoed.api.notification.create"]: {denizenId?: number, time?: {nanos: number}, status?: 'unread' | 'read', body?: any, title?: string, id?: number, type?: string};
        ["kotoed.api.denizen.create"]: {denizenId?: string, email?: string, id?: number};
        ["kotoed.api.submission.create"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.commentAggregates"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.history"]: {limit?: number, submissionId: number};
        ["kotoed.api.course.report"]: {id: number, timestamp?: number};
        ["kotoed.api.submission.comment.update"]: {authorId?: number, datetime?: number, sourcefile?: string, sourceline?: number, submissionId?: number, originalSubmissionId?: number, persistentCommentId?: number, previousCommentId?: number, text?: string, id?: number, state?: 'open' | 'closed'};
        ["kotoed.api.submission.tags.search"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.comment_template.read_all"]: {denizenId?: number, text?: string, name?: string, id?: number};
        ["kotoed.api.submission.result.read.batch"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withTags?: boolean, withVerificationData?: boolean};
        ["kotoed.api.denizen.profile.updatepassword"]: {initiatorDenizenId: string, initiatorPassword: string, newPassword: string, targetId: number};
        ["kotoed.api.buildTemplate.read"]: {commandLine?: any, environment?: any, id?: number};
        ["kotoed.api.oAuthProvider.list"]: void;
        ["kotoed.api.denizen.profile.update"]: {denizenId: string, email?: string, emailNotifications: boolean, firstName?: string, group?: string, id: number, lastName?: string, oauth?: Array<[string, string]>};
        ["kotoed.api.submission.commentsTotal"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.code.download"]: {submissionId: number};
        ["kotoed.api.denizen.search.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.submission.report"]: {id: number, timestamp?: number};
        ["kotoed.api.submission.result.read"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.project.read"]: {courseId?: number, denizenId?: number, repoType?: string, repoUrl?: string, deleted?: boolean, name?: string, id?: number};
        ["kotoed.api.notification.markRead"]: {denizenId?: number, time?: {nanos: number}, status?: 'unread' | 'read', body?: any, title?: string, id?: number, type?: string};
        ["kotoed.api.denizen.search"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.denizen.read"]: {denizenId?: string, email?: string, id?: number};
        ["kotoed.api.tag.delete"]: {style?: any, name?: string, id?: number};
        ["kotoed.api.submission.read"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.notification.read"]: {denizenId?: number, time?: {nanos: number}, status?: 'unread' | 'read', body?: any, title?: string, id?: number, type?: string};
        ["kotoed.api.submission.last"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.notification.current"]: {denizenId: number};
        ["kotoed.api.comment_template.delete"]: {denizenId?: number, text?: string, name?: string, id?: number};
        ["kotoed.api.course.update"]: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId?: number, icon?: string, name?: string, id?: number, state?: 'open' | 'frozen' | 'closed'};
        ["kotoed.api.submission.list"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withTags?: boolean, withVerificationData?: boolean};
        ["kotoed.api.notification.web.publicKey"]: void;
        ["kotoed.api.comment_template.update"]: {denizenId?: number, text?: string, name?: string, id?: number};
        ["kotoed.api.notification.markRead.all"]: {denizenId?: number, time?: {nanos: number}, status?: 'unread' | 'read', body?: any, title?: string, id?: number, type?: string};
        ["kotoed.api.course.remoteError"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.notification.web.subscribe"]: {auth: string, denizenId: number, endpoint: string, key: string};
        ["kotoed.api.course.verification.data"]: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId?: number, icon?: string, name?: string, id?: number, state?: 'open' | 'frozen' | 'closed'};
        ["kotoed.api.submission.tags.delete"]: {submissionId?: number, tagId?: number, id?: number};
        ["kotoed.api.notification.current.render"]: {denizenId: number};
        ["kotoed.api.submission.list.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withTags?: boolean, withVerificationData?: boolean};
        ["kotoed.api.build.status"]: {buildId: number};
        ["kotoed.api.buildTemplate.create"]: {commandLine: Array<{commandLine: Array<string>, type: 'SHELL'}>, environment: Array<{name: string, value: string}>, id: number};
        ["kotoed.api.tag.list"]: void;
        ["kotoed.api.course.search"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.submission.annotations"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.notification.render"]: {denizenId?: number, time?: {nanos: number}, status?: 'unread' | 'read', body?: any, title?: string, id?: number, type?: string};
        ["kotoed.api.course.read"]: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId?: number, icon?: string, name?: string, id?: number, state?: 'open' | 'frozen' | 'closed'};
        ["kotoed.api.comment_template.search"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.build.summary"]: void;
        ["kotoed.api.submission.comments"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.comment_template.search_count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.course.search.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.submission.verification.data"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.tags.read"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.remoteError"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.tags.create"]: {submissionId?: number, tagId?: number, id?: number};
        ["kotoed.api.project.verification.data"]: {courseId?: number, denizenId?: number, repoType?: string, repoUrl?: string, deleted?: boolean, name?: string, id?: number};
        ["kotoed.api.submission.comment.create"]: {authorId?: number, datetime?: number, sourcefile?: string, sourceline?: number, submissionId?: number, originalSubmissionId?: number, persistentCommentId?: number, previousCommentId?: number, text?: string, id?: number, state?: 'open' | 'closed'};
        ["kotoed.api.project.remoteError"]: {errors: Array<number>, status: 'Unknown' | 'NotReady' | 'Processed' | 'Invalid'};
        ["kotoed.api.submission.tags.search.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.project.delete"]: {courseId?: number, denizenId?: number, repoType?: string, repoUrl?: string, deleted?: boolean, name?: string, id?: number};
        ["kotoed.api.submission.update"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.tag.read"]: {style?: any, name?: string, id?: number};
        ["kotoed.api.buildTemplate.update"]: {commandLine: Array<{commandLine: Array<string>, type: 'SHELL'}>, environment: Array<{name: string, value: string}>, id: number};
        ["kotoed.api.project.searchForCourse.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withTags?: boolean, withVerificationData?: boolean};
        ["kotoed.api.submission.verification.clean"]: {datetime?: number, revision?: string, parentSubmissionId?: number, projectId?: number, id?: number, state?: 'pending' | 'invalid' | 'open' | 'obsolete' | 'closed' | 'deleted'};
        ["kotoed.api.submission.code.date"]: {fromLine?: number, path: string, submissionId: number, toLine?: number};
        ["kotoed.api.comment_template.create"]: {denizenId?: number, text?: string, name?: string, id?: number};
        ["kotoed.api.course.create"]: {baseRevision?: string, baseRepoUrl?: string, buildTemplateId?: number, icon?: string, name?: string, id?: number, state?: 'open' | 'frozen' | 'closed'};
        ["kotoed.api.project.searchForCourse"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withTags?: boolean, withVerificationData?: boolean};
        ["kotoed.api.tag.create"]: {style?: any, name?: string, id?: number};
        ["kotoed.api.denizen.profile.read"]: {denizenId?: string, email?: string, id?: number};
        ["kotoed.api.submission.comment.search.count"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.course.code.read"]: {courseId: number, path: string};
        ["kotoed.api.submission.code.read"]: {fromLine?: number, path: string, submissionId: number, toLine?: number};
        ["kotoed.api.submission.comment.search"]: {currentPage?: number, denizenId?: number, find?: any, pageSize?: number, text: string, withVerificationData?: boolean};
        ["kotoed.api.submission.code.list"]: {submissionId: number};
        ["kotoed.api.submission.comment.read"]: {authorId?: number, datetime?: number, sourcefile?: string, sourceline?: number, submissionId?: number, originalSubmissionId?: number, persistentCommentId?: number, previousCommentId?: number, text?: string, id?: number, state?: 'open' | 'closed'};
        ["kotoed.api.course.code.list"]: {courseId: number};
        [key: string]: any
    };
}
