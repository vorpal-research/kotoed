import {eventBus} from "../../eventBus";
import {RequestWithId, SubmissionIdRequest} from "./common";

const FETCH_COMMENTS_ADDRESS = "kotoed.api.submission.comments";
const CREATE_COMMENT_ADDRESS = "kotoed.api.submission.comment.create";

type CommentState = "open" | "closed";

interface BaseComment {
    submission_id: number
    text: string
    sourcefile: string
    sourceline: number
}

export interface BaseCommentToRead extends BaseComment {
    id: number
    author_id: number
    datetime: number
    state: CommentState
}

export interface CommentToRead extends BaseCommentToRead {
    denizen_id: string
}

export interface LineComments {
    line: number,
    comments: Array<CommentToRead>
}

export interface FileComments {
    by_line: Array<LineComments>,
    filename: string
}


export type CommentToPost = BaseComment

export interface PostCommentResponse {
    record: BaseCommentToRead
    // Do not care about everything else for now
}

export type ReviewComments = Array<FileComments>

type CommentsRequest = RequestWithId

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
        submission_id: submissionId,
        text,
        sourcefile,
        sourceline
    });
    return res.record;
}