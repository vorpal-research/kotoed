import * as React from "react";

import { Classes, ITreeNode,  Tree } from "@blueprintjs/core";
import {FileType} from "../model";
import {makeLoadingNode} from "../util/filetree";

export type FileTreeNode = LoadingNode | FileNode;

export interface LoadingNode extends ITreeNode {
    kind: "loading"
}

export interface FileNode extends ITreeNode {
    kind: "file"
    type: FileType
    childNodes?: Array<FileNode>
    filename: string
}

export interface FileTreeProps {
    loading: boolean
    nodes: Array<FileNode>
}

export interface FileTreeCallbacks {
    onDirExpand(path: number[])
    onDirCollapse(path: number[])
    onFileSelect(path: number[])
    onMount()
}

export default class FileTree extends React.Component<FileTreeProps & FileTreeCallbacks, {}> {
    // override @PureRender because nodes are not a primitive type and therefore aren't included in
    // shallow prop comparison
    public shouldComponentUpdate() {
        return true;
    }

    public componentDidMount() {
        this.props.onMount()
    }

    private onNodeClick = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "loading")
            return;
        if (nodeData.type === "file" && !nodeData.isSelected) {
            this.props.onFileSelect(path);
        } else if (nodeData.type === "directory" && nodeData.isExpanded) {
            this.props.onDirCollapse(path);
        } else if (nodeData.type === "directory" && !nodeData.isExpanded) {
            this.props.onDirExpand(path);
        }
    };

    private onNodeCollapse = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "file" && nodeData.type === "directory") {
            this.props.onDirCollapse(path);
        }
    };

    private onNodeExpand = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "file" && nodeData.type === "directory") {
            this.props.onDirExpand(path);
        }
    };

    render() {
        return (
            <Tree
                contents={this.props.loading ?
                    [makeLoadingNode(() => 0)] : // We don't care about id since it's only one node here
                    this.props.nodes}
                onNodeClick={this.onNodeClick}
                onNodeCollapse={this.onNodeCollapse}
                onNodeExpand={this.onNodeExpand}
                className={Classes.ELEVATION_0}
            />
        );
    }
}