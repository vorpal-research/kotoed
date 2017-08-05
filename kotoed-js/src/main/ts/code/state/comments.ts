import {List, Map} from "immutable";

import {CommentToRead} from "../remote/comments";

export interface CommentsState {
    comments: ReviewComments
    fetched: boolean
}

export type CommentState = "open" | "closed";


interface CommentRenderingProps {
    canStateBeChanged: boolean,
    canBeEdited: boolean
}

export type Comment = CommentToRead & CommentRenderingProps

// TODO is there a better way to create aliases for factory functions?
export type LineComments = List<Comment>
export const LineComments = () => List<Comment>();

export type FileComments = Map<number, LineComments>
export const FileComments = () => Map<number, LineComments>();

export type ReviewComments = Map<string, FileComments>
export const ReviewComments = () => Map<string, FileComments>();
