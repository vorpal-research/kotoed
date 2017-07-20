/**
 * Created by gagarski on 7/18/17.
 */
import * as React from "react"
import {File} from "../model";
import {FileNode, FileTreeNode, LoadingNode} from "../components/FileTree";
import {Spinner} from "@blueprintjs/core";

export type FileTreePath = Array<number>;

export function visitNodePath(fileTree: Array<FileNode>,
                              numPath: FileTreePath,
                              callback: (node: FileNode) => void): FileNode|null {
    let children = fileTree;
    let node = null;
    numPath.forEach((index) => {
        node = children[index];
        callback(node);
        children = node.childNodes; // will be undef for last but we don't care
    });
    return node;
}

export function getNodeAt(fileTree: Array<FileNode>,
                          numPath: FileTreePath): FileNode|null {
    return visitNodePath(fileTree, numPath, () => {});
}

export function nodePathToFilePath(fileTree: Array<FileNode>, numPath: FileTreePath): string {
    let path = [];
    visitNodePath(fileTree, numPath, node => path.push(node.filename));
    return path.join("/");
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

    if (idGen == null) {
        let id = 0;
        idGen = () => {
            return ++id;
        }
    }
    fileNodes.forEach((node) => {
        let bpNode: FileNode = {
            kind: "file",
            id: idGen(),
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