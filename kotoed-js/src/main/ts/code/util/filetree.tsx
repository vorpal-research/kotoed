/**
 * Created by gagarski on 7/18/17.
 */
import * as React from "react"
import {List, Map, Seq, Iterable} from "immutable"
import {StoredFile} from "../model";
import {FileNode, FileTreeNode, LoadingNode} from "../components/FileTree";
import {Spinner} from "@blueprintjs/core";
import {FileTree, ImmutableStoredFile} from "../state";

export type FileTreePath = List<number>;

export function nodePathToStoragePath(numPath: List<number>): List<number|string> {
    return numPath.map(val => val as number|string).toList().interpose("children").toList();
}

export function nodePathToFilePath(fileTree: FileTree, numPath: List<number>): string {
    let path = [];
    let children = fileTree;
    numPath.forEach((index) => {
        path.push(children.getIn([index, "filename"]));
        children = children.getIn([index, "children"]); // will be undef for last but we don't care
    });

    return path.join("/");
}

function makeLoadingNode(idGen: (() => number)): LoadingNode {
    return {
        kind: "loading",
        id: idGen(),
        label: <Spinner className="pt-small"/>
    }
}

export function toBlueprintTreeNodes(fileNodes: List<ImmutableStoredFile>, idGen: (() => number)|null = null): Array<FileTreeNode> {
    // TODO make Blueprint use Immutable.js or change storage format
    let ret: Array<FileTreeNode> = [];

    if (idGen == null) {
        let id = 0;
        idGen = () => {
            return ++id;
        }
    }
    fileNodes.forEach((node) => {
        let bpNode: FileNode = {
            kind: "filetree",
            id: idGen(),
            filename: node.get("filename"),
            isLoaded: node.get("isLoaded"),
            isExpanded: node.get("isExpanded"),
            isSelected: node.get("isSelected"),
            label: node.get("filename"),
            type: node.get("type"),
            hasCaret: node.get("type") == "directory",
            iconName: node.get("type") == "file" ? "pt-icon-document" : node.get("isExpanded") ? "pt-icon-folder-open" : "pt-icon-folder-close",
            childNodes: toBlueprintTreeNodes(node.get("children"), idGen)
        };
        // TODO Probably we should do it on dir fetch started, not here
        if (!node.get("isLoaded")) {
            bpNode.childNodes.push(makeLoadingNode(idGen));
        }
        ret.push(bpNode);
    });
    return ret;
}