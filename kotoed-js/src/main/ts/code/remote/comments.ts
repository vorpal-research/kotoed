import {eventBus} from "../../eventBus";
import {RequestWithId, SubmissionIdRequest} from "./common";
import {Kotoed} from "../../util/kotoed-api";
import {BaseComment, BaseCommentToRead, CommentToRead} from "../../data/comment";
import {CommentState} from "../state/comments";


export interface LineComments {
    line: number,
    comments: Array<CommentToRead>
}

export interface FileComments {
    byLine: Array<LineComments>,
    filename: string
}

export type CommentAggregate = {
    [state in CommentState]: number
}

export interface CommentAggregatesForFile {
    file: string
    aggregate: CommentAggregate
}

export interface CommentAggregates {
    byFile: Array<CommentAggregatesForFile>
    lost: CommentAggregate
}

export type ReviewComments = Array<FileComments>

export interface CommentsResponse {
    byFile: ReviewComments
    lost: Array<CommentToRead>
}

type CommentsRequest = RequestWithId
type CommentAggregatesRequest = RequestWithId

export type CommentToPost = BaseComment

export interface PostCommentResponse {
    record: BaseCommentToRead
    // Do not care about everything else for now
}

interface CommentStateUpdate {
    id: number
    state: CommentState
}

interface CommentEdit {
    id: number
    text: string
}

interface LastSeenResponse {
    location: BaseCommentToRead | null
}

export async function fetchComments(submissionId: number): Promise<CommentsResponse> {
    return eventBus.send<CommentsRequest, CommentsResponse>(Kotoed.Address.Api.Submission.Comments, {
        id: submissionId,
    });
}

export async function fetchCommentAggregates(submissionId: number): Promise<CommentAggregates> {
    return eventBus.send<CommentAggregatesRequest, CommentAggregates>(Kotoed.Address.Api.Submission.CommentAggregates, {
        id: submissionId,
    });
}

export async function postComment(submissionId: number,
                                  sourcefile: string,
                                  sourceline: number,
                                  text: string): Promise<BaseCommentToRead> {
    let res = await eventBus.send<CommentToPost, PostCommentResponse>(Kotoed.Address.Api.Submission.Comment.Create, {
        submissionId: submissionId,
        text,
        sourcefile,
        sourceline
    });
    return res.record;
}

export async function setCommentState(commentId: number,
                                      state: CommentState): Promise<BaseCommentToRead> {
    let res = await eventBus.send<CommentStateUpdate, PostCommentResponse>(Kotoed.Address.Api.Submission.Comment.Update, {
        id: commentId,
        state
    });
    return res.record;
}

export async function editComment(commentId: number,
                                  text: string): Promise<BaseCommentToRead> {
    let res = await eventBus.send<CommentEdit, PostCommentResponse>(Kotoed.Address.Api.Submission.Comment.Update, {
        id: commentId,
        text
    });
    return res.record;
}
