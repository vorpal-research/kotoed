import * as React from "react";

import {Classes, Tree} from "@blueprintjs/core";
import {FileTreeNode, FileTreeProps} from "../state/filetree";
import {makeLoadingNode} from "../util/filetree";

export interface FileTreeCallbacks {
    onDirExpand(path: number[]): void
    onDirCollapse(path: number[]): void
    onFileSelect(path: number[]): void
}

export default class FileTree extends React.Component<FileTreeProps & FileTreeCallbacks, {}> {
    private onNodeClick = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.data.kind === "loading")
            return;
        if (nodeData.data.type === "file" && !nodeData.isSelected) {
            this.props.onFileSelect(path);
        } else if (nodeData.data.type === "directory" && nodeData.isExpanded) {
            this.props.onDirCollapse(path);
        } else if (nodeData.data.type === "directory" && !nodeData.isExpanded) {
            this.props.onDirExpand(path);
        }
    };

    private onNodeCollapse = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.data.kind === "file" && nodeData.data.type === "directory") {
            this.props.onDirCollapse(path);
        }
    };

    private onNodeExpand = (nodeData: FileTreeNode, path: number[]) => {
        if (nodeData.data.kind === "file" && nodeData.data.type === "directory") {
            this.props.onDirExpand(path);
        }
    };

    render() {
        return (
            <Tree
                contents={this.props.loading ?
                    [makeLoadingNode(() => 0)] : // We don't care about id since it's only one node here
                    this.props.root.childNodes || []}
                onNodeClick={this.onNodeClick}
                onNodeCollapse={this.onNodeCollapse}
                onNodeExpand={this.onNodeExpand}
                className={`${Classes.ELEVATION_0} code-review-tree`}
            />
        );
    }
}