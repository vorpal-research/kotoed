import * as React from "react"
import {ITreeNode} from "@blueprintjs/core";
import {FileType} from "../remote/code";
import {Node, NodePatch, NodeProps} from "./blueprintTree";

export interface FileTreeProps {
    loading: boolean
    root: FileNode
}

export interface FileTreeState extends FileTreeProps {
    selectedPath: Array<number>
    aggregatesFetched: boolean
}

export type FileTreeNode = LoadingNode | FileNode;


export interface LoadingNode extends ITreeNode {
    data: {
        kind: "loading"
    }
}

export interface FileProps {
    kind: "file"
    type: FileType
    filename: string
    openComments: number
    closedComments: number
}


export type FileNode = Node<FileProps>
export type FileNodeProps = NodeProps<FileProps>
export type FileNodePatch = NodePatch<FileProps>

export function FileNode(props: FileNodeProps): FileNode {
    return Node<FileProps>(props);
}

