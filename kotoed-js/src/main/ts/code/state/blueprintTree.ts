import * as _ from "lodash"
import {ITreeNode} from "@blueprintjs/core";

export class TreeError extends Error {
    constructor(numPath: NodePath) {
        super(`Path ${numPath.join(".")} not found in tree.`);
    }
}

export interface NodeProps<T> extends ITreeNode {
    childNodes?: Array<NodeProps<T>>
    data: T
}

export interface NodeMethods<T> {
    getDataCopy: () => T
    patchTowards: (numPath: NodePath,
                   patcher: (node: Node<T>, path: NodePath) => NodePatch<T>) => Node<T>
    patchAt: (numPath: NodePath,
              patcher: (node: Node<T>) => NodePatch<T>) => Node<T>
    visitNodePath: (numPath: NodePath, callback: (node: Node<T>) => void) => void
    getNodeAt: (numPath: NodePath) => Node<T>
    withMutations: (mutator: (mutable: MutableNode<T>) => void) => Node<T>
    replaceChildAt:(numPath: NodePath,
                    newChild: NodeProps<T>) => Node<T>
    visitSubtreeAt: (numPath: NodePath,
                     callback: (node: MutableNode<T>) => boolean) => void
    expand: (numPath: NodePath) => Node<T>
    collapse: (numPath: NodePath) => Node<T>
    expandTowards: (numPath: NodePath) => Node<T>

    collapseTowards: (numPath: NodePath) => Node<T>
    select: (numPath: NodePath) => Node<T>
    unselect: (numPath: NodePath) => Node<T>
}

interface MutableNode<T> extends NodeProps<T>, Readonly<NodeMethods<T>> {
    childNodes?: Array<MutableNode<T>>
}

export interface Node<T> extends Readonly<MutableNode<T>> {
    readonly childNodes?: Array<Node<T>>
}

export function Node<T>(props: NodeProps<T>): Node<T> {
    return new NodeImpl<T>(props);
}

export type NodePatch<T> = Partial<NodeProps<T>>

export type NodePath = Array<number>;

export class NodeImpl<T> implements Node<T>, NodeMethods<T> {
    // Required
    readonly id: string | number;
    readonly label: string | JSX.Element;
    readonly data: Readonly<T>;

    //Optional
    readonly childNodes?: Array<Node<T>>;
    readonly className?: string;
    readonly hasCaret?: boolean;
    readonly iconName?: string;
    readonly isExpanded?: boolean;
    readonly isSelected?: boolean;
    readonly secondaryLabel?: string | JSX.Element;

    // internal
    private mutable: boolean;

    getDataCopy() {
        return _.cloneDeep(this.data) as T;
    };

    /**
     * Mutates state!
     */
    private applyPatch(patch: NodePatch<T>) {
        if (patch.childNodes) {
            for (let i = 0; i < patch.childNodes.length; i++) {
                if (!(patch.childNodes[i] instanceof NodeImpl)) {
                    patch.childNodes[i] = new NodeImpl<T>(patch.childNodes[i]);
                }
            }
        }
        Object.assign(this, patch);
    };

    constructor(props: NodeProps<T> | NodeImpl<T>, patch?: NodePatch<T>) {
        Object.assign(this, props);
        if (props instanceof NodeImpl && patch) {
            this.applyPatch(patch);
        } else if (!(props instanceof NodeImpl) && props.childNodes) {
            this.childNodes = [];
            for (let child of props.childNodes) {
                this.childNodes.push(new NodeImpl<T>(child))
            }
        }

    }

    private enableMutability() { this.mutable = true };
    private disableMutability() { this.mutable = false };

    patchTowards(numPath: NodePath,
                 patcher: (node: Node<T>, path: NodePath) => NodePatch<T>): Node<T> {
        function doPatch(node: NodeImpl<T>, numPathPos: number, mutable: boolean) {
            let curPath = numPath.slice(0, numPathPos);
            let patch = patcher(node, curPath);

            if (_.isEmpty(patch))
                return node;

            if (numPathPos !== numPath.length - 1 && patch.hasOwnProperty("childNodes"))
                throw new Error("You can only patch childNodes on last node on path");
            if (!mutable)
                return new NodeImpl(node, patch);
            else {
                node.applyPatch(patch);
                return node;
            }
        }

        function helper(current: NodeImpl<T>, numPathPos: number, mutable: boolean): NodeImpl<T> {
            let updated: NodeImpl<T>;
            let curPath = numPath.slice(0, numPathPos);
            if (numPathPos < numPath.length) {
                if (!current.childNodes)
                    throw new TreeError(numPath);
                let childIx = numPath[numPathPos];
                let child = current.childNodes[childIx];

                if (!child)
                    throw new TreeError(numPath);

                if (!(child instanceof NodeImpl))
                    throw new Error(`Bastard child at ${curPath}!`);

                let updatedChild = helper(child as NodeImpl<T>, numPathPos + 1, mutable);

                if (updatedChild === child)
                    updated = current;
                else {
                    let childNodes = [...current.childNodes];
                    childNodes[childIx] = updatedChild;
                    updated = new NodeImpl<T>(current, {
                        childNodes
                    })
                }
            } else {
                updated = current;
            }

            return doPatch(updated,
                numPathPos,
                mutable ||            // Set to mutable externally
                (updated !== current) // or we already have copied it
            );
        }

        return helper(this, 0, this.mutable);
    };

    patchAt(numPath: NodePath,
            patcher: (node: Node<T>) => NodePatch<T>): Node<T> {
        return this.patchTowards(numPath, (node, curPath) => {
            if (_.isEqual(curPath, numPath))
                return patcher(node);
            else return {};
        });
    };

    visitNodePath(numPath: NodePath,
                  callback: (node: Node<T>) => void) {
        let children = this.childNodes;

        let node: Node<T>;

        for (let ix of numPath) {
            if (!children)
                throw new TreeError(numPath);

            node = children[ix];

            if (!node)
                throw new TreeError(numPath);

            callback(node);

            if (node.childNodes)
                children = node.childNodes;
        }

    };


    getNodeAt(numPath: NodePath): Node<T> {
        let node: Node<T> = this;
        this.visitNodePath(numPath, (vNode) => { node = vNode });
        return node;
    };

    withMutations(mutator: (mutable: MutableNode<T>) => void): Node<T> {
        let copy = _.cloneDeep(this);
        copy.enableMutability();
        mutator(copy);
        copy.disableMutability();
        return copy
    };

    replaceChildAt(numPath: NodePath,
                   newChild: NodeProps<T> | NodeImpl<T>): Node<T> {
        let newChildImpl: NodeImpl<T>;
        if (!(newChild instanceof NodeImpl))
            newChildImpl = new NodeImpl<T>(newChild);
        else
            newChildImpl = newChild as NodeImpl<T>;

        if (numPath.length == 0)
            return newChildImpl;

        let init = numPath.slice(0, -1);
        let last = numPath[numPath.length - 1];

        return this.patchAt(init, (node) => {
            if (!node.childNodes)
                throw new TreeError(numPath);

            let childNodes = [...(node.childNodes as Array<NodeProps<T>>)];

            if (!childNodes[last])
                throw new TreeError(numPath);
            childNodes[last] = newChild;

            return {
                childNodes
            }
        });

    };

    visitSubtreeAt(numPath: NodePath,
                   callback: (node: MutableNode<T>) => boolean): Node<T> {
        function visitor(node: MutableNode<T>,
                         callback: (node: MutableNode<T>) => boolean
        ) {
            if (callback(node) && node.childNodes) {
                node.childNodes.forEach(child => visitor(child, callback));
            }
        }

        let node = this.getNodeAt(numPath);

        let newNode = node.withMutations((mutable) => {
            let node = mutable.getNodeAt(numPath);
            visitor(node, callback)
        });

        return this.replaceChildAt(numPath, newNode)
    };

    private setIsExpanded(numPath: NodePath, value: boolean): Node<T> {
        return this.patchAt(numPath, () => {
            return {
                isExpanded: value
            }
        })
    };

    private setIsExpandedTowards(numPath: NodePath, value: boolean): Node<T> {
        return this.patchTowards(numPath, () => {
            return {
                isExpanded: value
            }
        })
    };

    expand(numPath: NodePath): Node<T> {
        return this.setIsExpanded(numPath, true);
    };

    collapse(numPath: NodePath): Node<T> {
        return this.setIsExpanded(numPath, false);
    };

    expandTowards(numPath: NodePath): Node<T> {
        return this.setIsExpandedTowards(numPath, true);
    };

    collapseTowards(numPath: NodePath): Node<T> {
        return this.setIsExpandedTowards(numPath, false);
    };

    private setIsSelected(numPath: NodePath, value: boolean): Node<T> {
        return this.patchAt(numPath, () => {
            return {
                isSelected: value
            }
        })
    };

    select(numPath: NodePath): Node<T> {
        return this.setIsSelected(numPath, true);
    };

    unselect(numPath: NodePath): Node<T> {
        return this.setIsSelected(numPath, false);
    };



}
