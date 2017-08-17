import {CapabilitiesState} from "./state/capabilities";
import {FileComments, ReviewComments, LineComments, CommentsState, Comment, LostFoundComments} from "./state/comments";
import {EditorState} from "./state/editor";
import {FileNode, FileTreeState} from "./state/filetree";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {
    commentsFetch, commentPost, commentStateUpdate, dirCollapse, dirExpand, fileLoad,
    fileSelect,
    rootFetch, commentAggregatesFetch, aggregatesUpdate, capabilitiesFetch, hiddenCommentsExpand,
    expandedResetForFile, expandedResetForLine, commentEdit, fileUnselect, expandedResetForLostFound
} from "./actions";
import {
    ADD_DELTA,
    addAggregates, CLOSE_DELTA, makeFileNode, OPEN_DELTA, registerAddComment, registerCloseComment,
    registerOpenComment, updateAggregate
} from "./util/filetree";
import {NodePath} from "./state/blueprintTree";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "./remote/constants";
import {List} from "immutable";

const initialFileTreeState: FileTreeState = {
    root: FileNode({
        id: 0,
        label: "dummy",
        data: {
            kind: "file",
            filename: "dummy",
            type: "directory",
            aggregate: {
                open: 0,
                closed: 0
            }
        }
    }),
    loading: true,
    selectedPath: [],
    aggregatesLoading: true,
    lostFoundAggregate: {
        open: 0,
        closed: 0
    }
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
    } else if (isType(action, fileUnselect)) {
        let newState = {...state};
        newState.root =
            newState.root
                .unselect(newState.selectedPath);
        newState.selectedPath = NodePath();
        return newState;
    } else if (isType(action, rootFetch.started)) {
        let newState = {...state};
        newState.loading = true;
        return newState;
    } else if (isType(action, commentAggregatesFetch.started)) {
        let newState = {...state};
        newState.aggregatesLoading = true;
        return newState;
    } else if (isType(action, rootFetch.done)) {
        let newState = {...state};
        newState.root = makeFileNode(action.payload.result.root);
        newState.loading = false;
        return newState;
    } else if (isType(action, commentAggregatesFetch.done)) {
        let newState = {...state};
        newState.root = addAggregates(newState.root, action.payload.result);
        newState.lostFoundAggregate = action.payload.result.lost;
        newState.aggregatesLoading = false;
        return newState;
    } else if (isType(action, aggregatesUpdate)) {
        let newState = {...state};
        if (action.payload.file !== UNKNOWN_FILE) {
            switch (action.payload.type) {
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
        } else {
            switch (action.payload.type) {
                case "new":
                    newState.lostFoundAggregate = updateAggregate(newState.lostFoundAggregate, ADD_DELTA);
                    break;
                case "open":
                    newState.lostFoundAggregate = updateAggregate(newState.lostFoundAggregate, OPEN_DELTA);
                    break;
                case "close":
                    newState.lostFoundAggregate = updateAggregate(newState.lostFoundAggregate, CLOSE_DELTA);
                    break;

            }
        }
        return newState;
    }
    return state;
};

const defaultEditorState = {
    value: "",
    fileName: "",
    displayedComments: FileComments(),
    mode: {},
    loading: false
};

export const editorReducer = (state: EditorState = defaultEditorState, action: Action) => {
    if (isType(action, fileLoad.started)) {
        let newState = {...state};
        newState.loading = true;
        return newState;
    } else if (isType(action, fileLoad.done)) {
        let newState = {...state};
        newState.value = action.payload.result.value;
        newState.fileName = action.payload.params.filename;
        newState.loading = false;
        return newState;
    }
    return state;
};

export const defaultCommentsState = {
    comments: ReviewComments(),
    lostFound: LostFoundComments(),
    loading: true
};

function updateComment(reviewState: CommentsState, comment: Comment) {
    let {id, state, sourcefile, sourceline, text} = comment;
    let newState = {...reviewState};

    const doUpdate = (comments: List<Comment>) => {
        let oldCommentIx = comments.findIndex(c => !!c && (c.id == id));
        let oldComment = comments.get(oldCommentIx);
        if (!oldComment)
            throw new Error("Comment to update not found");

        let newComment = {...oldComment, text, state};

        return comments.set(oldCommentIx, newComment);
    };

    if (sourcefile !== UNKNOWN_FILE && sourceline !== UNKNOWN_LINE) {
        let comments = newState.comments.getIn([sourcefile, sourceline], LineComments()) as LineComments;

        comments = doUpdate(comments);

        newState.comments = reviewState.comments.setIn([sourcefile, sourceline], comments);
    } else {
        let comments = newState.lostFound;
        comments = doUpdate(comments);

        newState.lostFound = comments;
    }

    return newState;
}

function collapseIfClosed(comment: Comment) {
    return {...comment, collapsed: comment.state === "closed"}
}

export const commentsReducer = (reviewState: CommentsState = defaultCommentsState, action: Action) => {
    if (isType(action, commentsFetch.started)) {
        let newState = {...reviewState};
        newState.loading = true;
        return newState
    } else if (isType(action, commentsFetch.done)) {
        return action.payload.result;
    } else if (isType(action, commentPost.done)) {
        let {sourcefile, sourceline} = action.payload.result;
        let newState = {...reviewState};
        let comments = newState.comments.getIn([sourcefile, sourceline], LineComments()) as LineComments;
        comments = comments.push(action.payload.result);
        newState.comments = reviewState.comments.setIn([sourcefile, sourceline], comments);
        return newState;
    } else if (isType(action, commentStateUpdate.done)) {
        return updateComment(reviewState, action.payload.result);
    } else if (isType(action, commentEdit.done)) {
        return updateComment(reviewState, action.payload.result);
    } else if (isType(action, hiddenCommentsExpand)) {
        let {comments, file, line} = action.payload;
        let newState = {...reviewState};

        const doUpdate = (mutable: List<Comment>) => {
            comments.forEach((update: Comment) => {
                let {id} = update;
                let ix = mutable.findIndex((c: Comment) => c.id == id);
                if (ix === -1) {
                    throw new Error("Comment to update not found");
                }
                let newComment = {...mutable.get(ix)};
                newComment.collapsed = false;
                mutable.set(ix, newComment);
            });
        };
        if (file !== UNKNOWN_FILE && line !== UNKNOWN_LINE) {
            let newComments = (newState.comments.getIn([file, line]) as LineComments).withMutations((mutable) => {
                doUpdate(mutable)
            });
            newState.comments = newState.comments.setIn([file, line], newComments);
        } else {
            newState.lostFound = newState.lostFound.withMutations((mutable) => {
                doUpdate(mutable)
            });
        }
        return newState;
    } else if (isType(action, expandedResetForLine)) {
        let {file, line} = action.payload;
        let newState = {...reviewState};
        let newComments = (newState.comments.getIn([file, line], LineComments()) as LineComments)
            .map((comment: Comment) => {
                return {...comment, collapsed: comment.state === "closed"}
            }) as LineComments;
        newState.comments = newState.comments.setIn([file, line], newComments);
        return newState;
    } else if (isType(action, expandedResetForFile)) {
        let {file} = action.payload;
        let newState = {...reviewState};
        let newComments = newState.comments.get(file, FileComments()).map((lc: LineComments) => lc.map((comment: Comment) =>
            collapseIfClosed(comment)) as LineComments) as FileComments;  // "as Smth" is ugly but typings specify say that .map() returns iterable, however docs say that it returns concrete collection
        newState.comments = newState.comments.set(file, newComments);
        return newState;
    } else if (isType(action, expandedResetForLostFound)) {
        let newState = {...reviewState};
        newState.lostFound = newState.lostFound.map((comment: Comment) => collapseIfClosed(comment)) as LostFoundComments;
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
    loading: true
};

export const capabilitiesReducer = (state: CapabilitiesState = defaultCapabilitiesState, action: Action) => {
    if (isType(action, capabilitiesFetch.done)) {
        return {
            loading: false,
            capabilities: action.payload.result
        }
    }
    return state;
};