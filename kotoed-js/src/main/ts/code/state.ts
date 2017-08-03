import {List, Map} from "immutable";
import {FileTreeProps} from "./components/FileTree";
import moment = require("moment");
import {CmMode} from "./util/codemirror";
import {ITreeNode} from "@blueprintjs/core";
import {FileType} from "./remote/code";
import {CommentAggregate} from "./remote/comments";
import {Capabilities} from "./remote/capabilities";
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
    aggregatesFetched: boolean
}

export interface CommentsState {
    comments: ReviewComments
    fetched: boolean
}

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    commentsState: CommentsState
    capabilitiesState: CapabilitiesState
}

export interface CapabilitiesState {
    capabilities: Capabilities
    fetched: boolean
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

// TODO is there a better way to create aliases for factory functions?
export type LineComments = List<Comment>
export const LineComments = () => List<Comment>();

export type FileComments = Map<number, LineComments>
export const FileComments = () => Map<number, LineComments>();

export type ReviewComments = Map<string, FileComments>
export const ReviewComments = () => Map<string, FileComments>();

export type FileTreeNode = LoadingNode | FileNode;

export interface LoadingNode extends ITreeNode {
    kind: "loading"
}

export interface FileNode extends ITreeNode {
    kind: "file"
    type: FileType
    childNodes?: FileNodes
    filename: string
    openComments: number
    closedComments: number
}

export type FileNodes = Array<FileNode>
export const FileNodes = () => Array<FileNodes>();

export type FileTreePath = Array<number>;