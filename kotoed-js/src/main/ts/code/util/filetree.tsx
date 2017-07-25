/**
 * Created by gagarski on 7/18/17.
 */
import * as React from "react"
import {File, FileType} from "../model";
import {Spinner, ITreeNode} from "@blueprintjs/core";
import {FileNotFoundError} from "../errors";

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

export function visitNodePath(fileTree: FileNodes,
                              numPath: FileTreePath,
                              callback: (node: FileNode) => void): FileNode | null {
    let children: FileNodes = fileTree;

    let node: FileNode | null = null;
    numPath.forEach((index) => {
        node = children[index];
        callback(node);
        if (node.childNodes)
            children = node.childNodes;
    });
    return node;
}

export function getNodeAt(fileTree: Array<FileNode>,
                          numPath: FileTreePath): FileNode | null {
    return visitNodePath(fileTree, numPath, () => {});
}

export function nodePathToFilePath(fileTree: Array<FileNode>, numPath: FileTreePath): string {
    let path: Array<string> = [];
    visitNodePath(fileTree, numPath, node => path.push(node.filename));
    return path.join("/");
}

export function filePathToNodePath(fileTree: Array<FileNode>, filePath: string): FileTreePath {
    let pathChunks = filePath.split("/");
    let children = fileTree;
    let numPath: FileTreePath = [];
    for (let chunk of pathChunks) {
        if (chunk === "")
            continue;

        if (children === null)
            throw new FileNotFoundError(filePath);

        let childIx = children.findIndex((child) => child.filename === chunk);

        if (childIx === -1)
            throw new FileNotFoundError(filePath);

        let child = children[childIx];

        numPath.push(childIx);

        if (child.childNodes)
            children = child.childNodes;
    }
    return numPath
}

function setDirIsExpanded(fileTree: Array<FileNode>, numPath: FileTreePath, value: boolean): void {
    let node = getNodeAt(fileTree, numPath);
    if (node !== null) {
        node.isExpanded = value;
    }
}

export function collapseDir(fileTree: Array<FileNode>, numPath: FileTreePath): void {
    setDirIsExpanded(fileTree, numPath, false);
}

export function expandDir(fileTree: Array<FileNode>, numPath: FileTreePath): void {
    setDirIsExpanded(fileTree, numPath, true);
}

export function expandEverything(fileTree: Array<FileNode>, numPath: FileTreePath): void {
    visitNodePath(fileTree, numPath, node => {
        switch (node.type) {
            case "directory":
                node.isExpanded = true;
                break;
            case "file":
                node.isSelected = true;
                break;
        }
    });
}

function setFileIsSelected(fileTree: Array<FileNode>, numPath: FileTreePath, value: boolean): void {
    let node = getNodeAt(fileTree, numPath);
    if (node !== null && node.type === "file") {
        node.isSelected = value;
    }
}

export function selectFile(fileTree: Array<FileNode>, numPath: FileTreePath): void {
    setFileIsSelected(fileTree, numPath, true)
}

export function unselectFile(fileTree: Array<FileNode>, numPath: FileTreePath): void {
    setFileIsSelected(fileTree, numPath, false)
}

export function makeLoadingNode(idGen: (() => number)): LoadingNode {
    return {
        kind: "loading",
        id: idGen(),
        label: <Spinner className="pt-small"/>
    }
}

export function makeBlueprintTreeState(fileNodes: Array<File>, idGen: (() => number)|null = null): Array<FileNode> {
    let ret: Array<FileNode> = [];

    let id = 0;
    let idGenF = idGen ? idGen : () => {return ++id;};
    fileNodes.forEach((node) => {
        let bpNode: FileNode = {
            kind: "file",
            id: idGenF(),
            filename: node.name,
            isExpanded: false,
            isSelected: false,
            label: node.name,
            type: node.type,
            hasCaret: node.type === "directory",
            iconName: node.type == "file" ? "pt-icon-document" : "pt-icon-folder-close",
            childNodes: makeBlueprintTreeState(node.children || [], idGen)
        };
        ret.push(bpNode);
    });
    return ret;
}