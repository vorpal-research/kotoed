import {List, Map} from "immutable";
import {CommentToRead} from "../../data/comment";


export interface CommentsState {
    comments: ReviewComments
    lostFound: LostFoundComments
    loading: boolean
}

export type CommentState = "open" | "closed";


interface CommentRenderingProps {
    canStateBeChanged: boolean,
    canBeEdited: boolean
    collapsed: boolean
    processing: boolean
}

export type OptionalCommentToRead = { [K in keyof CommentToRead] ?: CommentToRead[K] } & { id: number, text: string  }
export type Comment = OptionalCommentToRead & CommentRenderingProps

// TODO is there a better way to create aliases for factory functions?
export type LineComments = List<Comment>
export const LineComments = () => List<Comment>();

export type FileComments = Map<number, LineComments>
export const FileComments = () => Map<number, LineComments>();

export type ReviewComments = Map<string, FileComments>
export const ReviewComments = () => Map<string, FileComments>();

export type LostFoundComments = List<Comment>
export const LostFoundComments = () => List<Comment>();
