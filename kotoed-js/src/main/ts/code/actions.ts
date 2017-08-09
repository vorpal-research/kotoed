import {List, Map} from "immutable";


import actionCreatorFactory from 'typescript-fsa';
import {
    CodeReviewState
} from "./state";
import {
    Comment, CommentsState, CommentState, FileComments, LineComments, ReviewComments
} from "./state/comments";
import {fetchRootDir, fetchFile, File} from "./remote/code";
import {FileNotFoundError} from "./errors";
import {push} from "react-router-redux";
import {Dispatch} from "redux";
import {addRenderingProps, commentsResponseToState} from "./util/comments";
import {
    CommentAggregates,
    fetchCommentAggregates,
    fetchComments,
    postComment as doPostComment,
    setCommentState as doSetCommentState,
    editComment as doEditComment, UNKNOWN_FILE, UNKNOWN_LINE

} from "./remote/comments";
import {Capabilities, fetchCapabilities} from "./remote/capabilities";
import {getFilePath, getNodePath} from "./util/filetree";
import {NodePath} from "./state/blueprintTree";
import {makeCodePath, makeLostFoundPath} from "./containers/CodeReviewContainer";
const actionCreator = actionCreatorFactory();

interface SubmissionPayload {
    submissionId: number
}

interface NodePathPayload {
    treePath: NodePath
}

interface FilePathPayload {
    filename: string
}

interface DirFetchResult {
    root: File
}

interface FileFetchResult {
    value: string,
    displayedComments: FileComments
}

interface PostCommentPayload {
    submissionId: number
    text: string
    sourcefile: string
    sourceline: number
}

interface CommentStatePayload {
    commentId: number,
    state: CommentState
}

interface CommentEditPayload {
    commentId: number,
    newText: string
}

interface AggregatesUpdatePayload {
    file: string
    type: "new" | "close" | "open"
}

interface HiddenCommentsExpandPayload {
    file: string
    line: number
    comments: List<Comment>
}

interface ExpandedResetForLinePayload {
    file: string
    line: number
}

interface ExpandedResetForFilePayload {
    file: string
}

interface GoToLastSeenPayload {
    id: number
}

// Local actions
export const dirExpand = actionCreator<NodePathPayload>('DIR_EXPAND');
export const dirCollapse = actionCreator<NodePathPayload>('DIR_COLLAPSE');
export const fileSelect = actionCreator<NodePathPayload>('FILE_SELECT');
export const fileUnselect = actionCreator<{}>('FILE_UNSELECT');
export const aggregatesUpdate = actionCreator<AggregatesUpdatePayload>("AGGREGATES_UPDATE");
export const hiddenCommentsExpand = actionCreator<HiddenCommentsExpandPayload>("HIDDEN_COMMENTS_EXPAND");
export const expandedResetForLine = actionCreator<ExpandedResetForLinePayload>("EXPANDED_RESET_FOR_LINE");
export const expandedResetForFile = actionCreator<ExpandedResetForFilePayload>("EXPANDED_RESET_FOR_FILE");
export const expandedResetForLostFound = actionCreator<{}>("EXPANDED_RESET_FOR_LOST_FOUND");

// File or dir fetch actions
export const rootFetch = actionCreator.async<SubmissionPayload, DirFetchResult, {}>('ROOT_FETCH');
export const fileLoad = actionCreator.async<FilePathPayload & SubmissionPayload, FileFetchResult, {}>('FILE_LOAD');

// Comment fetch actions
export const commentsFetch = actionCreator.async<SubmissionPayload, CommentsState, {}>('COMMENT_FETCH');
export const commentAggregatesFetch =
    actionCreator.async<SubmissionPayload, CommentAggregates, {}>("COMMENT_AGGREGATES_FETCH");
export const commentPost = actionCreator.async<PostCommentPayload, Comment, {}>('COMMENT_POST');
export const commentStateUpdate = actionCreator.async<CommentStatePayload, Comment>('COMMENT_STATE_UPDATE');
export const commentEdit = actionCreator.async<CommentEditPayload, Comment>('COMMENT_EDIT');


// Capabilities
export const capabilitiesFetch = actionCreator.async<{}, Capabilities, {}>('CAPABILITIES_FETCH');


export function loadCode(payload: SubmissionPayload & FilePathPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {
        await fetchCapabilitiesIfNeeded(payload)(dispatch, getState);
        await fetchRootDirIfNeeded(payload)(dispatch, getState);
        await fetchCommentsIfNeeded(payload)(dispatch, getState);
        await expandAndLoadIfNeeded(payload)(dispatch, getState);
        await fetchCommentAggregatesIfNeeded(payload)(dispatch, getState);
    }
}

export function loadLostFound(payload: SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {
        await fetchCapabilitiesIfNeeded(payload)(dispatch, getState);
        await fetchRootDirIfNeeded(payload)(dispatch, getState);
        await fetchCommentsIfNeeded(payload)(dispatch, getState);
        await fetchCommentAggregatesIfNeeded(payload)(dispatch, getState);
        dispatch(fileUnselect({}));
    }
}


export function fetchRootDirIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().fileTreeState.loading)
            return Promise.resolve();

        dispatch(rootFetch.started({
            submissionId: payload.submissionId
        }));

        return fetchRootDir(payload.submissionId).then((root) => {
            dispatch(rootFetch.done({
                params: {
                    submissionId: payload.submissionId
                },
                result: {
                    root
                }
            }))
        });
    };
}

export function expandAndLoadIfNeeded(payload: SubmissionPayload & FilePathPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let numPath: NodePath;
        try {
            numPath = getNodePath(getState().fileTreeState.root, payload.filename);
        } catch(e) {
            if (e instanceof FileNotFoundError) {
                numPath = [];
                console.warn(e);
            } else {
                throw e;
            }
        }

        if (numPath === getState().fileTreeState.selectedPath)
            return;

        let node = getState().fileTreeState.root.getNodeAt(numPath);

        if (node === null)
            return;

        switch (node.data.type) {
            case "directory":
                dispatch(dirExpand({
                    treePath: numPath
                }));
                break;
            case "file":
                dispatch(fileSelect({
                    treePath: numPath
                }));
                loadFileToEditor({
                    filename: payload.filename,
                    submissionId: payload.submissionId
                })(dispatch, getState);

                break;
        }
    }
}

export function unselectFile() {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(fileUnselect({}));
    }
}


export function setCodePath(payload: NodePathPayload & SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let filename = getFilePath(getState().fileTreeState.root, payload.treePath);
        dispatch(push(makeCodePath(payload.submissionId, filename)))
    }
}

export function setLostFoundPath(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(push(makeLostFoundPath(payload.submissionId)))
    }
}


export function loadFileToEditor(payload: FilePathPayload & SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let {filename} = payload;
        dispatch(fileLoad.started({
            submissionId: payload.submissionId,
            filename
        }));  // Not used yet
        // TODO save file locally?

        resetExpandedForFile({
            file: filename
        })(dispatch, getState);

        fetchFile(payload.submissionId, filename).then(result => {
            dispatch(fileLoad.done({
                params: {
                    submissionId: payload.submissionId,
                    filename
                },
                result: {
                    value: result,
                    displayedComments: getState().commentsState.comments.get(filename, Map<number, LineComments>()),
                }
            }));
        });

    }
}

export function fetchCommentsIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {

        if (!getState().commentsState.loading)
            return;

        dispatch(commentsFetch.started({
            submissionId: payload.submissionId
        }));  // Not used yet

        return fetchComments(payload.submissionId).then(result => {
            dispatch(commentsFetch.done({
                params: {
                    submissionId: payload.submissionId,
                },
                result: commentsResponseToState(result, getState().capabilitiesState.capabilities)
            }));
        });

    }
}

export function fetchCommentAggregatesIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {

        if (!getState().fileTreeState.aggregatesLoading)
            return;

        dispatch(commentAggregatesFetch.started({
            submissionId: payload.submissionId
        }));  // Not used yet

        return fetchCommentAggregates(payload.submissionId).then(result => {
            dispatch(commentAggregatesFetch.done({
                params: {
                    submissionId: payload.submissionId,
                },
                result
            }));
        });

    }
}

export function postComment(payload: PostCommentPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(commentPost.started(payload));  // Not used yet

        let result = await doPostComment(payload.submissionId, payload.sourcefile, payload.sourceline, payload.text);

        dispatch(commentPost.done({
            params: payload,
            result: addRenderingProps({
                ...result,
                denizenId: getState().capabilitiesState.capabilities.principal.denizenId,
            }, getState().capabilitiesState.capabilities)
        }));

        dispatch(aggregatesUpdate({
            type: "new",
            file: result.sourcefile
        }));
    }
}

export function setCommentState(payload: CommentStatePayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(commentStateUpdate.started(payload));  // Not used yet

        let result = await doSetCommentState(payload.commentId, payload.state);

        dispatch(commentStateUpdate.done({
            params: payload,
            result: addRenderingProps({
                    ...result,
                    denizenId: getState().capabilitiesState.capabilities.principal.denizenId,  // Will be overriden by reducer
                },
                getState().capabilitiesState.capabilities)
        }));
        dispatch(aggregatesUpdate({
            type: result.state === "open" ? "open" : "close",
            file: result.sourcefile
        }));

    }
}

export function editComment(payload: CommentEditPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(commentEdit.started(payload));  // Not used yet

        let result = await doEditComment(payload.commentId, payload.newText);

        dispatch(commentEdit.done({
            params: payload,
            result: addRenderingProps({
                    ...result,
                    denizenId: getState().capabilitiesState.capabilities.principal.denizenId,  // Will be overriden by reducer
                },
                getState().capabilitiesState.capabilities)
        }));

    }
}


export function fetchCapabilitiesIfNeeded(payload: SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().capabilitiesState.loading)
            return;

        dispatch(capabilitiesFetch.started({}));  // Not used yet

        let result = await fetchCapabilities(payload.submissionId);

        dispatch(capabilitiesFetch.done({
            params: {},
            result
        }));
    }
}

export function expandHiddenComments(payload: HiddenCommentsExpandPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(hiddenCommentsExpand(payload));
    }
}

export function resetExpandedForLine(payload: ExpandedResetForLinePayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(expandedResetForLine(payload));
    }
}

export function resetExpandedForFile(payload: ExpandedResetForFilePayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(expandedResetForFile(payload));
    }
}

export function resetExpandedForLostFound() {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(expandedResetForLostFound({}));
    }
}
