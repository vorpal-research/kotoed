
import {CommentState} from "../state";
import {eventBus} from "../../eventBus";
import {RequestWithId, SubmissionIdRequest} from "./common";

const COMMENTS_ADDRESS = "kotoed.api.submission.comments";

export interface Comment {
    id: number,
    author_id: number,
    denizen_id: string,
    datetime: number,
    state: CommentState,
    text: string
}

export interface LineComments {
    line: number,
    comments: Array<Comment>
}

export interface FileComments {
    by_line: Array<LineComments>,
    filename: string
}

export type ReviewComments = Array<FileComments>

type CommentsRequest = RequestWithId

export async function fetchComments(submissionId: number): Promise<ReviewComments> {
    return eventBus.send<CommentsRequest, ReviewComments>(COMMENTS_ADDRESS, {
        id: submissionId,
    });
}