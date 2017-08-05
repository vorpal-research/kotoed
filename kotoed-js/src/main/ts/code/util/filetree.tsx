import * as React from "react"
import {File} from "../remote/code";
import {Spinner} from "@blueprintjs/core";
import {FileNotFoundError, FileTreeError} from "../errors";
import {FileNode, FileNodes, FileTreePath, LoadingNode} from "../state/filetree";
import {CommentAggregate, CommentAggregates} from "../remote/comments";



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

export function visitSubtree(fileTree: FileNode,
                             callback: (node: FileNode) => boolean): void {
    if (callback(fileTree) && fileTree.childNodes) {
        fileTree.childNodes.forEach(child => visitSubtree(child, callback));
    }
}

export function getNodeAt(fileTree: FileNodes,
                          numPath: FileTreePath): FileNode | null {
    return visitNodePath(fileTree, numPath, () => {});
}

export function nodePathToFilePath(fileTree: Array<FileNode>, numPath: FileTreePath): string {
    let path: Array<string> = [];
    visitNodePath(fileTree, numPath, node => path.push(node.filename));
    return path.join("/");
}

export function filePathToNodePath(fileTree: FileNodes, filePath: string): FileTreePath {

    function normalizeSlashes(path: string): string {
        path = path.replace(/\/+/g, '/');

        if (path.length === 0)
            return path;

        path = path[0] === "/" ? path.slice(1) : path;

        if (path.length === 0)
            return path;

        path = path[path.length - 1] === "/" ? path.slice(0, -1) : path;

        return path;
    }

    let leftToProcess = normalizeSlashes(filePath);
    let children = fileTree;
    let numPath: FileTreePath = [];
    while (leftToProcess !== "") {
        if (children === null)
            throw new FileNotFoundError(filePath);

        let fileIx = children.findIndex((child) => leftToProcess === child.filename);
        let dirIx = children.findIndex((child) => leftToProcess.startsWith(normalizeSlashes(child.filename) + "/"));
        let childIx = fileIx !== -1 ? fileIx : dirIx;

        if (childIx === -1)
            throw new FileNotFoundError(filePath);

        let child = children[childIx];

        numPath.push(childIx);

        if (child.childNodes)
            children = child.childNodes;
        else
            children = [];

        if (fileIx !== -1)
            leftToProcess = leftToProcess.slice(normalizeSlashes(child.filename).length);
        else
            leftToProcess = leftToProcess.slice(normalizeSlashes(child.filename).length + 1);
    }
    return numPath
}

function setDirIsExpanded(fileTree: FileNodes, numPath: FileTreePath, value: boolean): void {
    let node = getNodeAt(fileTree, numPath);
    if (node !== null) {
        node.isExpanded = value;
    }
}

export function collapseDir(fileTree: FileNodes, numPath: FileTreePath): void {
    setDirIsExpanded(fileTree, numPath, false);
}

export function expandDir(fileTree: FileNodes, numPath: FileTreePath): void {
    setDirIsExpanded(fileTree, numPath, true);
}

export function expandEverything(fileTree: FileNodes, numPath: FileTreePath): void {
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

export function collapseEverything(fileTree: FileNodes, numPath: FileTreePath): void {
    let toCollapse = getNodeAt(fileTree, numPath);

    if (toCollapse === null)
        throw new FileTreeError(numPath);

    visitSubtree(toCollapse, node => {
        if (node.type === "file")
            return false;
        // If we collapse using only this function then this should work fine
        // Otherwise this won't collapse everything
        if (node.type === "directory" && !node.isExpanded)
            return false;

        node.isExpanded = false;
        return true;
    })
}

function setFileIsSelected(fileTree: FileNodes, numPath: FileTreePath, value: boolean): void {
    let node = getNodeAt(fileTree, numPath);
    if (node !== null && node.type === "file") {
        node.isSelected = value;
    }
}

export function selectFile(fileTree: FileNodes, numPath: FileTreePath): void {
    setFileIsSelected(fileTree, numPath, true)
}

export function unselectFile(fileTree: FileNodes, numPath: FileTreePath): void {
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
            childNodes: makeBlueprintTreeState(node.children || [], idGen),
            openComments: 0,
            closedComments: 0
        };
        ret.push(bpNode);
    });
    return ret;
}

export function makeNodeSecondaryLabel({openComments, closedComments}: FileNode) {
    if (openComments == 0 && closedComments == 0)
        return undefined;

    let labelClass = openComments === 0 ? "label-default" : "label-danger";

    return (<span className={`comments-counter label ${labelClass}`}>
        {openComments + closedComments}
    </span>)
}

export function addCommentAggregatesToFileTree(fileTree: FileNodes, aggregates: CommentAggregates) {
    for (let fileAgg of aggregates.byFile) {
        let nodePath;
        try {
            nodePath = filePathToNodePath(fileTree, fileAgg.file);
        }
        catch(e) {
            continue; // It's okay! We may have a directory that has been collapsed
            // TODO deal with exception hierarchy
            // TODO Now I have no idea how to check instanceof for Errors
            // https://github.com/Microsoft/TypeScript/issues/12581
            // https://github.com/babel/babel/issues/4485
            // https://github.com/Microsoft/TypeScript-wiki/blob/master/Breaking-Changes.md#extending-built-ins-like-error-array-and-map-may-no-longer-work
        }
        let node = getNodeAt(fileTree, nodePath);

        if (node === null)
            throw new FileTreeError(nodePath);

        node.openComments = fileAgg.aggregate["open"];
        node.closedComments = fileAgg.aggregate["closed"];
        node.secondaryLabel = makeNodeSecondaryLabel(node);
    }
}

export function updateCommentAggregates(fileTree: FileNodes, path: string, delta: CommentAggregate) {
    let nodePath = filePathToNodePath(fileTree, path);

    visitNodePath(fileTree, nodePath, (node) => {
        node.openComments += delta.open;
        node.closedComments += delta.closed;
        node.secondaryLabel = makeNodeSecondaryLabel(node)
    });
}

export function registerAddComment(fileTree: FileNodes, path: string) {
    updateCommentAggregates(fileTree, path, {open: 1, closed: 0})
}

export function registerOpenComment(fileTree: FileNodes, path: string) {
    updateCommentAggregates(fileTree, path, {open: 1, closed: -1})
}

export function registerCloseComment(fileTree: FileNodes, path: string) {
    updateCommentAggregates(fileTree, path, {open: -1, closed: 1})
}
