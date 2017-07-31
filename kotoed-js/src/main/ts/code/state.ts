import {List, Map} from "immutable";
import {FileTreeProps} from "./components/FileTree";
import moment = require("moment");
/**
 * Created by gagarski on 7/18/17.
 */

export interface EditorState {
    fileName: string
    value: string
}

export interface FileTreeState extends FileTreeProps {
    selectedPath: Array<number>
}

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    comments: ReviewComments
}

export type CommentState = "open" | "closed";

export interface Comment {
    id: number
    text: string
    dateTime: moment.MomentInput
    authorName: string,
    authorId: number,
    state: CommentState
}

export type LineCommentsState = List<Comment>
export type FileComments = Map<number, LineCommentsState>
export type ReviewComments = Map<string, FileComments>
