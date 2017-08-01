import * as _ from "lodash"
import {List, Map} from "immutable";

import {EditorState, FileComments, FileTreeState, LineCommentsState, ReviewComments, Comment} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {commentFetch, commentPost, dirCollapse, dirExpand, dirFetch, fileFetch, fileSelect, rootFetch} from "./actions";
import {
    collapseEverything, expandEverything, makeBlueprintTreeState, selectFile,
    unselectFile
} from "./util/filetree";

const initialFileTreeState: FileTreeState = {
    nodes: [],
    loading: true,
    selectedPath: []
};



export const fileTreeReducer = (state: FileTreeState = initialFileTreeState, action: Action) => {
    if (isType(action, dirExpand)) {
        let newState = _.cloneDeep(state);
        expandEverything(newState.nodes, action.payload.treePath);
        return newState;
    } else if (isType(action, dirCollapse)) {
        let newState = _.cloneDeep(state);
        collapseEverything(newState.nodes, action.payload.treePath);
        return newState;
    } else if (isType(action, fileSelect)) {
        let newState = _.cloneDeep(state);
        unselectFile(newState.nodes, newState.selectedPath);
        expandEverything(newState.nodes, action.payload.treePath);
        selectFile(newState.nodes, action.payload.treePath);
        newState.selectedPath = action.payload.treePath;
        return newState;
    } else if (isType(action, rootFetch.done)) {
        let newState = {...state};
        newState.nodes = makeBlueprintTreeState(action.payload.result.list);
        newState.loading = false;
        return newState;
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

export const commentsReducer = (reviewState: ReviewComments | null = null, action: Action) => {
    if (isType(action, commentFetch.done)) {
        return action.payload.result;
    } else if (reviewState !== null && isType(action, commentPost.done)) {
        let {id, state, sourcefile, sourceline, text, authorId, dateTime} = action.payload.result;
        let comments = reviewState.getIn([sourcefile, sourceline], List<Comment>()) as List<Comment>;
        comments = comments.push({
            authorId,
            state,
            authorName: "Me",  // TODO replace with proper name
            id,
            text,
            dateTime
        });
        reviewState = reviewState.setIn([sourcefile, sourceline], comments);
        return reviewState;
    }
    return reviewState;
};