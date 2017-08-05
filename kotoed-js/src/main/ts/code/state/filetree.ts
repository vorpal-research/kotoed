import moment = require("moment");
import {ITreeNode} from "@blueprintjs/core";
import {FileType} from "../remote/code";

export interface FileTreeProps {
    loading: boolean
    nodes: FileNodes
}

export interface FileTreeState extends FileTreeProps {
    selectedPath: Array<number>
    aggregatesFetched: boolean
}

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