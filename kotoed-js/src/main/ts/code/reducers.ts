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
    expandedResetForFile, expandedResetForLine, commentEdit, fileUnselect, expandedResetForLostFound, commentEmphasize,
    submissionFetch, annotationsFetch, commentTemplateFetch, fileDiff, diffFetch
} from "./actions";
import {
    ADD_DELTA,
    addAggregates, applyDiffToFileTree, CLOSE_DELTA, makeFileNode, OPEN_DELTA, registerAddComment, registerCloseComment,
    registerOpenComment, updateAggregate
} from "./util/filetree";
import {NodePath} from "./state/blueprintTree";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "./remote/constants";
import {List, Map} from "immutable";
import {DbRecordWrapper} from "../data/verification";
import {SubmissionToRead} from "../data/submission";
import {SubmissionState} from "./state/submission";
import {DEFAULT_FORM_STATE, FileForms, ReviewForms} from "./state/forms";
import {CodeAnnotationsState, ReviewAnnotations} from "./state/annotations";
import {CommentTemplateState} from "./state/templates";
import {CommentTemplates} from "./remote/templates";
import {DiffBase, fetchDiff, FileDiffResult} from "./remote/code";
import {DiffState} from "./state/diff";

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
        // Do not copy the state since it has not been published yet
        applyDiffToFileTree(newState.root, action.payload.result.diff, false);
        newState.loading = false;
        return newState;
    } else if (isType(action, diffFetch.done)) {
        let newState = {...state}
        newState.root = FileNode(applyDiffToFileTree(state.root, action.payload.result))
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

const defaultEditorState: EditorState = {
    value: "",
    fileName: "",
    displayedComments: FileComments(),
    loading: false,
    diff: []
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
    } else if (isType(action, fileDiff.done)) {
        let diff = action.payload.result;
        if (diff === undefined) return state;
        let newState = {...state};
        newState.diff = diff.changes;
        return newState;
    }
    return state;
};

const defaultDiffState: DiffState = {
    diff: Map<string, FileDiffResult>(),
    loading: false,
    base: {
        type: "PREVIOUS_CLOSED"
    }
}

function fileDiffToMap(diff: Array<FileDiffResult>) {
    return Map<string, FileDiffResult>().withMutations(mutable => {
        for (const diffEntry of diff) {
            mutable.set(diffEntry.toFile, diffEntry)
        }
    })

}

export const diffReducer = (state: DiffState = defaultDiffState, action: Action): DiffState => {
    if (isType(action, diffFetch.done)) {
        return {
            loading: false,
            base: action.payload.params.diffBase,
            diff: fileDiffToMap(action.payload.result)
        }
    } else if (isType(action, diffFetch.started)) {
        return {
            loading: true,
            base: action.payload.diffBase,
            diff: state.diff
        }
    } else if (isType(action, rootFetch.done)) {
        return {
            loading: false,
            base: state.base,
            diff: fileDiffToMap(action.payload.result.diff)
        }
    }
    return state
}

export const defaultCommentsState = {
    comments: ReviewComments(),
    lostFound: LostFoundComments(),
    loading: true
};


function updateComment(reviewState: CommentsState, comment: Partial<Comment>) {
    let {id, sourcefile, sourceline, text} = comment;
    let newState = {...reviewState};

    const doUpdate = (comments: List<Comment>) => {
        let oldCommentIx = comments.findIndex(c => !!c && (c.id == id));
        let oldComment = comments.get(oldCommentIx);
        if (!oldComment)
            throw new Error("Comment to update not found");

        let newComment = {...oldComment, ...comment};

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
    } else if (isType(action, commentStateUpdate.started) || isType(action, commentEdit.started)) {
        return updateComment(reviewState, {...action.payload, processing: true});
    } else if (isType(action, commentStateUpdate.done)) {
        return updateComment(reviewState, {...action.payload.result, processing: false});
    } else if (isType(action, commentEdit.done)) {
        return updateComment(reviewState, {...action.payload.result, processing: false});
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
    } else if (isType(action, commentEmphasize)) {
        let {file, line, commentId} = action.payload;
        let newState = {...reviewState};

        const doUpdateComment = (comment: Comment): Comment => {
            if (comment.id == commentId)
                return {...comment, collapsed: false};
            else
                return {...comment, collapsed: true};
        };
        if (file !== UNKNOWN_FILE && line !== UNKNOWN_LINE) {
            let newComments = newState.comments.getIn([file, line]).map((c: Comment) => doUpdateComment(c));
            newState.comments = newState.comments.setIn([file, line], newComments);
        } else {
            newState.lostFound = newState.lostFound.map((c: Comment) => doUpdateComment(c)) as List<Comment>;
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

export const defaultCodeAnnotationsState: CodeAnnotationsState = {
    loading: true,
    annotations: ReviewAnnotations()
};

export const annotationsReducer = (state: CodeAnnotationsState = defaultCodeAnnotationsState, action: Action) => {
    if(isType(action, annotationsFetch.done)) {
        return { loading: false, annotations: action.payload.result };
    }
    return state;
};

export const defaultCommentTemplateState: CommentTemplateState = {
    loading: true,
    templates: CommentTemplates()
};

export const commentTemplateReducer = (state: CommentTemplateState = defaultCommentTemplateState, action: Action) => {
    if(isType(action, commentTemplateFetch.done)) {
        return { loading: false, templates: action.payload.result }
    }
    return state
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
            postComment: false,
            changeState: false,
            resubmit: false,
            clean: false,
            tags: false,
            klones: false
        }
    },
    loading: true,
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

const defaultSubmissionState: SubmissionState = {
    submission: null
};

export const submissionReducer = (state: SubmissionState = defaultSubmissionState, action: Action): SubmissionState => {
    if (isType(action, submissionFetch.done)) {
        return {...state, submission: action.payload.result}
    }
    return state;
};


export const defaultFormState: ReviewForms = ReviewForms();

export const formReducer = (state: ReviewForms = defaultFormState, action: Action) => {
    // Smart casting
    if (!isType(action, commentPost.started) && !isType(action, commentPost.done))
        return state;

    let {sourcefile, sourceline} = isType(action, commentPost.started) ? action.payload : action.payload.params;
    let processing;

    if (isType(action, commentPost.started)) {
        processing = true
    } else if (isType(action, commentPost.done)) {
        processing = false;
    }


    // Cleaning up
    if (processing === false) {
        let forFile = state.get(sourcefile);

        if (forFile === undefined)
            return state;

        forFile = forFile.remove(sourceline);

        if (forFile.isEmpty()) {
            return state.remove(sourcefile);
        } else {
            return state.set(sourcefile, forFile);
        }
    } else {
        return state.setIn([sourcefile, sourceline], {processing});
    }
};
