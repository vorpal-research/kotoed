import {StoredFile} from "./model";
import {List, Map} from "immutable";
/**
 * Created by gagarski on 7/18/17.
 */

// TODO amount of data structures representing dirs and files is too damn high. Fix it.


export type ImmutableStoredFile = Map<string, any>;
export type FileTree = List<ImmutableStoredFile>;

// TODO maybe convert everything to immutable.js

export interface EditorState {
    fileName: string
    value: string
}

export interface FileTreeState {
    fileTree: FileTree,
    selectedPath: List<number>|null
}

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
}
