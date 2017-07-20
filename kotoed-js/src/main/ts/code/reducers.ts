import {EditorState, FileTree, FileTreeState} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {dirCollapse, dirExpand, dirFetch, fileFetch, fileSelect} from "./actions";
import {initialFileTreeState, rootDir} from "./data_stubs";
import {List} from "immutable";
import {nodePathToStoragePath} from "./util/filetree";
import {strictEqual} from "assert";
import {StoredFile} from "./model";



function setExpanded(state: FileTreeState, treePath: List<number>, val: boolean) {
    let newState = {...state};
    let expandedPath = nodePathToStoragePath(treePath).concat("isExpanded");
    let oldExpanded = state.fileTree.getIn(expandedPath);
    newState.fileTree = newState.fileTree.setIn(expandedPath, val);
    return newState;
}

function selectFile(state: FileTreeState, treePath: List<number>) {
    let newState = {...state};
    if (state.selectedPath) {
        let oldSelectedPath = nodePathToStoragePath(state.selectedPath).concat("isSelected");
        newState.fileTree = newState.fileTree.setIn(oldSelectedPath, false)
    }
    let selectedPath = nodePathToStoragePath(treePath).concat("isSelected");
    newState.fileTree = newState.fileTree.setIn(selectedPath, true);
    newState.selectedPath = treePath;
    return newState;
}

function addLoadedChildren(state: FileTreeState, dir: List<number>, children: List<StoredFile>) {
    let newState = {...state};
    let isLoadedPath = nodePathToStoragePath(dir).concat("isLoaded");
    let childrenPath = nodePathToStoragePath(dir).concat("children");

    newState.fileTree = newState.fileTree.setIn(isLoadedPath, true).setIn(childrenPath, children);
    return newState;

}

export const fileTreeReducer = (state: FileTreeState = initialFileTreeState, action: Action) => {
    if (isType(action, dirExpand)) {
        return setExpanded(state, action.payload.treePath, true);
    } else if (isType(action, dirCollapse)) {
        return setExpanded(state, action.payload.treePath, false);
    } else if (isType(action, fileSelect)) {
        return selectFile(state, action.payload.treePath);
    } else if (isType(action, dirFetch.done)) {
        return addLoadedChildren(state, action.payload.params.treePath, action.payload.result.list);
    }
    return state;
};

export const editorReducer = (state: EditorState = {value: "", fileName: ""}, action: Action) => {
    if (isType(action, fileFetch.done)) {
        let newState = {...state};
        newState.value = action.payload.result.value;
        newState.fileName = action.payload.params.filename;
        return newState;
    }
    return state;
};