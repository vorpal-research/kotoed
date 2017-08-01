import * as _ from "lodash"
import {List, Map} from "immutable";

import {
    EditorState, FileComments, FileTreeState, ReviewComments, Comment,
    LineComments, CommentsState
} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {
    commentFetch, commentPost, dirCollapse, dirExpand, dirFetch, editorCommentsUpdate, fileLoad, fileSelect,
    rootFetch
} from "./actions";
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

const defaultEditorState = {
    value: "",
    fileName: "",
    displayedComments: Map<number, LineComments>(),
    mode: {}
};

export const editorReducer = (state: EditorState = defaultEditorState, action: Action) => {
    if (isType(action, fileLoad.done)) {
        let newState = {...state};
        newState.value = action.payload.result.value;
        newState.fileName = action.payload.params.filename;
        newState.displayedComments = action.payload.result.displayedComments;
        newState.mode = action.payload.result.mode
        return newState;
    } else if (isType(action, editorCommentsUpdate)) {
        let newState = {...state};
        newState.displayedComments = action.payload;
        return newState
    }
    return state;
};

export const defaultCommentsState = {
    comments: Map<string, FileComments>(),
    commentsFetched: false
};

export const commentsReducer = (reviewState: CommentsState = defaultCommentsState, action: Action) => {
    if (isType(action, commentFetch.done)) {
        let newState = {...reviewState};
        newState.commentsFetched = true;
        newState.comments = action.payload.result;
        return newState;
    } else if (reviewState.commentsFetched && isType(action, commentPost.done)) {
        let {id, state, sourcefile, sourceline, text, authorId, dateTime} = action.payload.result;
        let newState = {...reviewState};
        let comments = newState.comments.getIn([sourcefile, sourceline], List<Comment>()) as List<Comment>;
        comments = comments.push({
            authorId,
            state,
            authorName: "Me",  // TODO replace with proper name
            id,
            text,
            dateTime
        });
        newState.comments = reviewState.comments.setIn([sourcefile, sourceline], comments);
        return newState;
    }
    return reviewState;
};