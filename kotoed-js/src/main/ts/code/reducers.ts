import * as _ from "lodash"

import {CapabilitiesState} from "./state/capabilities";
import {FileComments, ReviewComments, LineComments, CommentsState} from "./state/comments";
import {EditorState} from "./state/editor";
import {FileNode, FileTreeState} from "./state/filetree";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {
    commentsFetch, commentPost, commentStateUpdate, dirCollapse, dirExpand, editorCommentsUpdate, fileLoad,
    fileSelect,
    rootFetch, commentAggregatesFetch, aggregatesUpdate, capabilitiesFetch
} from "./actions";
import {
    addAggregates, makeFileNode, registerAddComment, registerCloseComment,
    registerOpenComment
} from "./util/filetree";


const initialFileTreeState: FileTreeState = {
    root: FileNode({
        id: 0,
        label: "dummy",
        data: {
            kind: "file",
            filename: "dummy",
            type: "file",
            closedComments: 0,
            openComments: 0
        }
    }),
    loading: true,
    selectedPath: [],
    aggregatesFetched: false
};

export const fileTreeReducer = (state: FileTreeState = initialFileTreeState, action: Action) => {
    if (isType(action, dirExpand)) {
        let newState = {...state};
        newState.root = newState.root.expandTowards(action.payload.treePath);
        return newState;
    } else if (isType(action, dirCollapse)) {
        let newState = {...state};
        newState.root = newState.root.collapse(action.payload.treePath);
        return newState;
    } else if (isType(action, fileSelect)) {
        let newState = {...state};
        newState.root =
            newState.root
                .unselect(newState.selectedPath)
                .expandTowards(action.payload.treePath)
                .select(action.payload.treePath);
        newState.selectedPath = action.payload.treePath;
        return newState;
    } else if (isType(action, rootFetch.done)) {
        let newState = {...state};
        newState.root = makeFileNode(action.payload.result.root);
        newState.loading = false;
        return newState;
    } else if (isType(action, commentAggregatesFetch.done)) {
        let newState = {...state};
        newState.aggregatesFetched = true;
        newState.root = addAggregates(newState.root, action.payload.result);
        return newState;
    } else if (isType(action, aggregatesUpdate)) {
        let newState = {...state};
        switch(action.payload.type) {
            case "new":
                newState.root = registerAddComment(newState.root, action.payload.file);
                break;
            case "open":
                newState.root = registerOpenComment(newState.root, action.payload.file);
                break;
            case "close":
                newState.root = registerCloseComment(newState.root, action.payload.file);
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
        let {sourcefile, sourceline} = action.payload.result;
        let newState = {...reviewState};
        let comments = newState.comments.getIn([sourcefile, sourceline], LineComments()) as LineComments;
        comments = comments.push(action.payload.result);
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