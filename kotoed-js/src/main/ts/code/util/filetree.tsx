import * as React from "react"
import {Button, Panel, Label, OverlayTrigger, Tooltip} from "react-bootstrap";
import {File} from "../remote/code";
import {IconClasses, Intent, Spinner} from "@blueprintjs/core";
import {CommentAggregate, CommentAggregates} from "../remote/comments";
import {FileNode, FileNodeProps, FileTreeProps, LoadingNode} from "../state/filetree";
import {NodePath} from "../state/blueprintTree";
import {FileNotFoundError} from "../errors";
import {ICON} from "@blueprintjs/core/dist/common/classes";

export function makeLoadingNode(idGen: (() => number)): LoadingNode {
    return {
        data: {
            kind: "loading"
        },
        id: idGen(),
        label: <Spinner className="pt-small"/>
    }
}

export function makeFileTreeProps(file: File, idGen: (() => number)|null = null): FileNodeProps {
    let id = 0;
    let idGenF = idGen ? idGen : () => {return ++id;};

    let iconType = (file.changed) ? "changed " : "";
    let nodeClass = (file.changed) ? "pt-tree-node-changed" : "";

    let bpNode: FileNodeProps = {
        id: idGenF(),
        isExpanded: false,
        isSelected: false,
        label: file.name,
        hasCaret: file.type === "directory",
        className: nodeClass,
        iconName: iconType + (file.type == "file" ? IconClasses.DOCUMENT : IconClasses.FOLDER_CLOSE),
        childNodes: [],
        data: {
            kind: "file",
            filename: file.name,
            type: file.type,
            aggregate: {
                open: 0,
                closed: 0
            }
        }
    };

    if (file.children) {
        file.children.forEach((child) => {
            bpNode.childNodes!.push(makeFileTreeProps(child, idGenF));
        });
    }
    return bpNode;
}

export function makeFileNode(file: File) {
    return FileNode(makeFileTreeProps(file));
}

export function makeAggregatesLabel(aggregate: CommentAggregate) {
    if (aggregate.open == 0 && aggregate.closed == 0)
        return undefined; // TODO maybe null?

    let labelClass = aggregate.open === 0 ? "default" : "danger";

    let tooltipText = `Unresolved comments: ${aggregate.open} Resolved comments: ${aggregate.closed}`;

    return (
        <OverlayTrigger placement="right" overlay={<Tooltip id="comment-aggregates-tooltip">{tooltipText}</Tooltip>}>
            <Label className="comments-counter" bsStyle={labelClass}>
                {aggregate.open + aggregate.closed}
            </Label>
        </OverlayTrigger>)
}


export function getFilePath(root: FileNode, numPath: NodePath): string {
    let path: Array<string> = [];
    root.visitNodePath(numPath, node => path.push(node.data.filename));
    return path.join("/");
}

export function getNodePath(root: FileNode, filePath: string): NodePath {
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
    let children = root.childNodes;
    let numPath: NodePath = [];
    while (leftToProcess !== "") {
        if (children === undefined)
            throw new FileNotFoundError(filePath);

        let fileIx = children.findIndex((child) => leftToProcess === child.data.filename);
        let dirIx = children.findIndex((child) =>
            leftToProcess.startsWith(normalizeSlashes(child.data.filename) + "/"));
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
            leftToProcess = leftToProcess.slice(normalizeSlashes(child.data.filename).length);
        else
            leftToProcess = leftToProcess.slice(normalizeSlashes(child.data.filename).length + 1);
    }
    return numPath
}

export function updateAggregate(aggregate: CommentAggregate, delta: CommentAggregate): CommentAggregate {
    return {
        open: aggregate.open + delta.open,
        closed: aggregate.closed + delta.closed
    }
}

export function addAggregates(root: FileNode, aggregates: CommentAggregates): FileNode {
    return root.withMutations( (node: FileNode) => {
        for (let fileAgg of aggregates.byFile) {
            let nodePath: NodePath;
            try {
                nodePath = getNodePath(node, fileAgg.file)
            }
            catch (e) {
                continue; // It's okay! We may have a directory that has been collapsed
                // TODO deal with exception hierarchy
                // TODO Now I have no idea how to check instanceof for Errors
                // https://github.com/Microsoft/TypeScript/issues/12581
                // https://github.com/babel/babel/issues/4485
                // https://github.com/Microsoft/TypeScript-wiki/blob/master/Breaking-Changes.md#extending-built-ins-like-error-array-and-map-may-no-longer-work
            }

            node.patchAt(nodePath, (patchedNode: FileNode) => {
                let data = patchedNode.getDataCopy();
                data.aggregate = updateAggregate(data.aggregate, fileAgg.aggregate);
                let secondaryLabel = makeAggregatesLabel(data.aggregate);
                return {
                    data,
                    secondaryLabel
                }
            });
        }
    });
}



export function updateAggregates(root: FileNode, path: string, delta: CommentAggregate): FileNode {
    let nodePath = getNodePath(root, path);

    return root.patchTowards(nodePath, (node: FileNode) => {
        let data = node.getDataCopy();
        data.aggregate = updateAggregate(data.aggregate, delta);
        let secondaryLabel = makeAggregatesLabel(data.aggregate);
        return {
            data,
            secondaryLabel
        }
    });
}

export const ADD_DELTA = {open: 1, closed: 0};
export const OPEN_DELTA = {open: 1, closed: -1};
export const CLOSE_DELTA = {open: -1, closed: 1};


export function registerAddComment(root: FileNode, path: string) {
    return updateAggregates(root, path, ADD_DELTA)
}

export function registerOpenComment(root: FileNode, path: string) {
    return updateAggregates(root, path, OPEN_DELTA)
}

export function registerCloseComment(root: FileNode, path: string) {
    return updateAggregates(root, path, CLOSE_DELTA)
}
