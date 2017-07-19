import * as React from "react";

import { Classes, ITreeNode,  Tree } from "@blueprintjs/core";

export type FileTreeNode = LoadingNode | FileNode;

export interface LoadingNode extends ITreeNode {
    kind: "loading"
}

export interface FileNode extends ITreeNode {
    kind: "filetree"
    type: "file" | "directory"
    childNodes?: Array<FileTreeNode | LoadingNode>
    filename: string
    isLoaded: boolean
}

export interface FileTreeProps {
    nodes: Array<FileTreeNode | LoadingNode>;
    // onNodeClick: (nodeData: FileTreeNode, path: number[]) => void
    // onNodeCollapse: (nodeData: FileTreeNode, path: number[]) => void
    // onNodeExpand: (nodeData: FileTreeNode, path: number[]) => void
    onDirExpand(path: number[])
    onDirCollapse(path: number[])
    onFileSelect(path: number[])

}

export default class FileTree extends React.Component<FileTreeProps, {}> {
    // override @PureRender because nodes are not a primitive type and therefore aren't included in
    // shallow prop comparison
    public shouldComponentUpdate() {
        return true;
    }

    private onNodeClick = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "filetree") {
            if (nodeData.type === "file" && !nodeData.isSelected) {
                this.props.onFileSelect(path);
            } else if (nodeData.type === "directory" && nodeData.isExpanded) {
                this.props.onDirCollapse(path);
            } else if (nodeData.type === "directory" && !nodeData.isExpanded) {
                this.props.onDirExpand(path);
            }
        }
    };

    private onNodeCollapse = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "filetree" && nodeData.type === "directory") {
            this.props.onDirCollapse(path);
        }
    };

    private onNodeExpand = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.kind === "filetree" && nodeData.type === "directory") {
            this.props.onDirExpand(path);
        }
    };

    render() {
        return (
            <Tree
                contents={this.props.nodes}
                onNodeClick={this.onNodeClick}
                onNodeCollapse={this.onNodeCollapse}
                onNodeExpand={this.onNodeExpand}
                className={Classes.ELEVATION_0}
            />
        );
    }
}