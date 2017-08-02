import {eventBus} from "../../eventBus";
import {RequestWithId, SubmissionIdRequest} from "./common";

const FETCH_COMMENTS_ADDRESS = "kotoed.api.submission.comments";
const CREATE_COMMENT_ADDRESS = "kotoed.api.submission.comment.create";
const UPDATE_COMMENT_ADDRESS = "kotoed.api.submission.comment.update"

type CommentState = "open" | "closed";

interface BaseComment {
    submissionId: number
    text: string
    sourcefile: string
    sourceline: number
}

export interface BaseCommentToRead extends BaseComment {
    id: number
    authorId: number
    datetime: number
    state: CommentState
}

export interface CommentToRead extends BaseCommentToRead {
    denizenId: string
}

export interface LineComments {
    line: number,
    comments: Array<CommentToRead>
}

export interface FileComments {
    byLine: Array<LineComments>,
    filename: string
}


export type CommentToPost = BaseComment

export interface PostCommentResponse {
    record: BaseCommentToRead
    // Do not care about everything else for now
}

export type ReviewComments = Array<FileComments>

type CommentsRequest = RequestWithId

interface CommentStateUpdate {
    id: number
    state: CommentState
}

export async function fetchComments(submissionId: number): Promise<ReviewComments> {
    return eventBus.send<CommentsRequest, ReviewComments>(FETCH_COMMENTS_ADDRESS, {
        id: submissionId,
    });
}

export async function postComment(submissionId: number,
                                  sourcefile: string,
                                  sourceline: number,
                                  text: string): Promise<BaseCommentToRead> {
    let res = await eventBus.send<CommentToPost, PostCommentResponse>(CREATE_COMMENT_ADDRESS, {
        submissionId: submissionId,
        text,
        sourcefile,
        sourceline
    });
    return res.record;
}

export async function setCommentState(commentId: number,
                                      state: CommentState): Promise<BaseCommentToRead> {
    let res = await eventBus.send<CommentStateUpdate, PostCommentResponse>(UPDATE_COMMENT_ADDRESS, {
        id: commentId,
        state
    });
    return res.record;
}