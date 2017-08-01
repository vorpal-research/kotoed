import {List, Map} from "immutable";
import {FileTreeProps} from "./components/FileTree";
import moment = require("moment");
import {CmMode} from "./util/codemirror";
import {ITreeNode} from "@blueprintjs/core";
import {FileType} from "./remote/code";
/**
 * Created by gagarski on 7/18/17.
 */

export interface EditorState {
    fileName: string
    value: string,
    displayedComments: FileComments
    mode: CmMode
}

export interface FileTreeState extends FileTreeProps {
    selectedPath: Array<number>
}

export interface CommentsState {
    comments: ReviewComments
    commentsFetched: boolean
}

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    commentsState: CommentsState
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

export type LineComments = List<Comment>
export type FileComments = Map<number, LineComments>
export type ReviewComments = Map<string, FileComments>

export type FileTreeNode = LoadingNode | FileNode;

export interface LoadingNode extends ITreeNode {
    kind: "loading"
}

export interface FileNode extends ITreeNode {
    kind: "file"
    type: FileType
    childNodes?: FileNodes
    filename: string
}

export type FileNodes = Array<FileNode>
export type FileTreePath = Array<number>;