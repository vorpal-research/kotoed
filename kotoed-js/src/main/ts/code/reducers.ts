import * as _ from "lodash"

import {
    EditorState, FileComments, FileTreeState, ReviewComments, LineComments, CommentsState,
    CapabilitiesState
} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {
    commentsFetch, commentPost, commentStateUpdate, dirCollapse, dirExpand, dirFetch, editorCommentsUpdate, fileLoad,
    fileSelect,
    rootFetch, commentAggregatesFetch, aggregatesUpdate, capabilitiesFetch
} from "./actions";
import {
    addCommentAggregatesToFileTree, collapseDir,
    collapseEverything, expandEverything, makeBlueprintTreeState, registerAddComment, registerCloseComment,
    registerOpenComment, selectFile,
    unselectFile
} from "./util/filetree";
import {Capabilities} from "./remote/capabilities";

const initialFileTreeState: FileTreeState = {
    nodes: [],
    loading: true,
    selectedPath: [],
    aggregatesFetched: false
};

export const fileTreeReducer = (state: FileTreeState = initialFileTreeState, action: Action) => {
    if (isType(action, dirExpand)) {
        let newState = _.cloneDeep(state);  // TODO try to do better
        expandEverything(newState.nodes, action.payload.treePath);
        return newState;
    } else if (isType(action, dirCollapse)) {
        let newState = _.cloneDeep(state);
        collapseDir(newState.nodes, action.payload.treePath);
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
    } else if (isType(action, commentAggregatesFetch.done)) {
        let newState = _.cloneDeep(state);
        newState.aggregatesFetched = true;
        addCommentAggregatesToFileTree(newState.nodes, action.payload.result);
        return newState;
    } else if (isType(action, aggregatesUpdate)) {
        let newState = _.cloneDeep(state);
        switch(action.payload.type) {
            case "new":
                registerAddComment(newState.nodes, action.payload.file);
                break;
            case "open":
                registerOpenComment(newState.nodes, action.payload.file);
                break;
            case "close":
                registerCloseComment(newState.nodes, action.payload.file);
                break;

        }
        return newState;
    }
    return state;
};

const defaultEditorState = {
    value: "",
    fileName: "",
    displayedComments: FileComments(),
    mode: {}
};

export const editorReducer = (state: EditorState = defaultEditorState, action: Action) => {
    if (isType(action, fileLoad.done)) {
        let newState = {...state};
        newState.value = action.payload.result.value;
        newState.fileName = action.payload.params.filename;
        newState.displayedComments = action.payload.result.displayedComments;
        newState.mode = action.payload.result.mode;
        return newState;
    } else if (isType(action, editorCommentsUpdate)) {
        let newState = {...state};
        newState.displayedComments = action.payload;
        return newState
    }
    return state;
};

export const defaultCommentsState = {
    comments: ReviewComments(),
    fetched: false
};

export const commentsReducer = (reviewState: CommentsState = defaultCommentsState, action: Action) => {
    if (isType(action, commentsFetch.done)) {
        let newState = {...reviewState};
        newState.fetched = true;
        newState.comments = action.payload.result;
        return newState;
    } else if (isType(action, commentPost.done)) {
        let {id, state, sourcefile, sourceline, text, authorId, denizenId, datetime} = action.payload.result;
        let newState = {...reviewState};
        let comments = newState.comments.getIn([sourcefile, sourceline], LineComments()) as LineComments;
        comments = comments.push({
            authorId,
            state,
            authorName: denizenId,  // TODO replace with proper name
            id,
            text,
            dateTime: datetime
        });
        newState.comments = reviewState.comments.setIn([sourcefile, sourceline], comments);
        return newState;
    } else if (isType(action, commentStateUpdate.done)) {
        let {id, state, sourcefile, sourceline, text} = action.payload.result;
        let newState = {...reviewState};
        let comments = newState.comments.getIn([sourcefile, sourceline], LineComments()) as LineComments;

        let oldCommentIx = comments.findIndex(c => !!c && (c.id == id));
        let oldComment = comments.get(oldCommentIx);
        if (!oldComment)
            throw new Error("Comment to update not found");

        let comment = {...oldComment, text, state};

        comments = comments.set(oldCommentIx, comment);
        newState.comments = reviewState.comments.setIn([sourcefile, sourceline], comments);
        return newState;
    }
    return reviewState;
};

export const defaultCapabilitiesState: CapabilitiesState = {
    capabilities: {
        principal: {
            denizenId: "???",
            id: -1
        },
        permissions: {
            editOwnComments: false,
            editAllComments: false,
            changeStateOwnComments: false,
            changeStateAllComments: false,
            postComment: false
        }
    },
    fetched: false
};

export const capabilitiesReducer = (state: CapabilitiesState = defaultCapabilitiesState, action: Action) => {
    if (isType(action, capabilitiesFetch.done)) {
        return {
            fetched: true,
            capabilities: action.payload.result
        }
    }
    return state;
};