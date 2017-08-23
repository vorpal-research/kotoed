

// TODO move all Kotoed entity interfaces here

import {WithId} from "./common";

type CommentState = "open" | "closed";

// Contains fields required to post
export interface BaseComment {
    submissionId: number
    text: string
    sourcefile: string
    sourceline: number
}

// All fields for comment DB entity
export interface BaseCommentToRead extends BaseComment, WithId {
    authorId: number
    datetime: number
    state: CommentState
    original?: BaseCommentToRead
}

// + denizenId
export interface CommentToRead extends BaseCommentToRead {
    denizenId: string
}