import {List, Map} from "immutable";


import actionCreatorFactory from 'typescript-fsa';
import {
    filePathToNodePath, getNodeAt, nodePathToFilePath
} from "./util/filetree";
import {
    CodeReviewState, CommentState, FileComments, FileTreePath, LineComments,
    ReviewComments
} from "./state";
import {waitTillReady, fetchRootDir, fetchFile, File} from "./remote/code";
import {FileNotFoundError} from "./errors";
import {push} from "react-router-redux";
import {Dispatch} from "redux";
import {commentsResponseToState} from "./util/comments";
import {
    CommentAggregates,
    CommentToRead, fetchCommentAggregates,
    fetchComments,
    postComment as doPostComment,
    setCommentState as doSetCommentState
} from "./remote/comments";
import {CmMode, guessCmMode} from "./util/codemirror";
import {Capabilities, fetchCapabilities} from "./remote/capabilities";
const actionCreator = actionCreatorFactory();

interface SubmissionPayload {
    submissionId: number
}

interface NodePathPayload {
    treePath: FileTreePath
}

interface FilePathPayload {
    filename: string
}

interface DirFetchResult {
    list: Array<File>
}

interface FileFetchResult {
    value: string,
    displayedComments: FileComments
    mode: CmMode
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

interface AggregatesUpdatePayload {
    file: string
    type: "new" | "close" | "open"
}

// Local actions
export const dirExpand = actionCreator<NodePathPayload>('DIR_EXPAND');
export const dirCollapse = actionCreator<NodePathPayload>('DIR_COLLAPSE');
export const fileSelect = actionCreator<NodePathPayload>('FILE_SELECT');
export const editorCommentsUpdate = actionCreator<FileComments>('EDITOR_COMMENTS_UPDATE');
export const aggregatesUpdate = actionCreator<AggregatesUpdatePayload>("AGGREGATES_UPDATE");

// File or dir fetch actions
export const dirFetch = actionCreator.async<NodePathPayload, DirFetchResult, {}>('DIR_FETCH');
export const rootFetch = actionCreator.async<SubmissionPayload, DirFetchResult, {}>('ROOT_FETCH');
export const fileLoad = actionCreator.async<FilePathPayload & SubmissionPayload, FileFetchResult, {}>('FILE_LOAD');

// Comment fetch actions
export const commentsFetch = actionCreator.async<SubmissionPayload, ReviewComments, {}>('COMMENT_FETCH');
export const commentAggregatesFetch =
    actionCreator.async<SubmissionPayload, CommentAggregates, {}>("COMMENT_AGGREGATES_FETCH");
export const commentPost = actionCreator.async<PostCommentPayload, CommentToRead, {}>('COMMENT_POST');
export const commentStateUpdate = actionCreator.async<CommentStatePayload, CommentToRead>('COMMENT_STATE_UPDATE');

// Capabilities
export const capabilitiesFetch = actionCreator.async<{}, Capabilities, {}>('CAPABILITIES_FETCH');

export function initialize(payload: SubmissionPayload & FilePathPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {
        await fetchCapabilitiesIfNeeded()(dispatch, getState);
        await fetchRootDirIfNeeded(payload)(dispatch, getState);
        await fetchCommentsIfNeeded(payload)(dispatch, getState);
        await expandAndLoadIfNeeded(payload)(dispatch, getState);
        await fetchCommentAggregatesIfNeeded(payload)(dispatch, getState);
    }
}

export function fetchRootDirIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().fileTreeState.loading)
            return Promise.resolve();

        dispatch(rootFetch.started({
            submissionId: payload.submissionId
        }));

        return fetchRootDir(payload.submissionId).then((fileList) => {
            dispatch(rootFetch.done({
                params: {
                    submissionId: payload.submissionId
                },
                result: {
                    list: fileList
                }
            }))
        });
    };
}

export function expandAndLoadIfNeeded(payload: SubmissionPayload & FilePathPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let numPath: FileTreePath;
        try {
            numPath = filePathToNodePath(getState().fileTreeState.nodes, payload.filename);
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

        let node = getNodeAt(getState().fileTreeState.nodes, numPath);

        if (node === null)
            return;

        switch (node.type) {
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

export function setPath(payload: NodePathPayload & SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let filename = nodePathToFilePath(getState().fileTreeState.nodes, payload.treePath);
        dispatch(push(`/${payload.submissionId}/${filename}`))
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
        fetchFile(payload.submissionId, filename).then(result => {
            dispatch(fileLoad.done({
                params: {
                    submissionId: payload.submissionId,
                    filename
                },
                result: {
                    value: result,
                    displayedComments: getState().commentsState.comments.get(filename, Map<number, LineComments>()),
                    mode: guessCmMode(filename)
                }
            }));
        });

    }
}

export function updateEditorComments() {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let filename = getState().editorState.fileName;
        dispatch(editorCommentsUpdate(getState().commentsState.comments.get(filename, Map<number, LineComments>())));
    }
}

export function fetchCommentsIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {

        if (getState().commentsState.fetched)
            return;

        dispatch(commentsFetch.started({
            submissionId: payload.submissionId
        }));  // Not used yet

        return fetchComments(payload.submissionId).then(result => {
            dispatch(commentsFetch.done({
                params: {
                    submissionId: payload.submissionId,
                },
                result: commentsResponseToState(result)
            }));
        });

    }
}

export function fetchCommentAggregatesIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {

        if (getState().fileTreeState.aggregatesFetched)
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
            result: {
                id: result.id,
                denizenId: getState().capabilitiesState.capabilities.principal.denizenId,
                submissionId: result.submissionId,
                authorId: result.authorId,
                datetime: result.datetime,
                state: result.state,
                sourcefile: result.sourcefile,
                sourceline: result.sourceline,
                text: result.text
            }
        }));

        updateEditorComments()(dispatch, getState);

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
            result: {
                id: result.id,
                denizenId: getState().capabilitiesState.capabilities.principal.denizenId,  // Will be overriden by reducer
                submissionId: result.submissionId,
                authorId: result.authorId,
                datetime: result.datetime,
                state: result.state,
                sourcefile: result.sourcefile,
                sourceline: result.sourceline,
                text: result.text
            }
        }));

        updateEditorComments()(dispatch, getState);

        dispatch(aggregatesUpdate({
            type: result.state === "open" ? "open" : "close",
            file: result.sourcefile
        }));

    }
}

export function fetchCapabilitiesIfNeeded() {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (getState().capabilitiesState.fetched)
            return;

        dispatch(capabilitiesFetch.started({}));  // Not used yet

        let result = await fetchCapabilities();

        dispatch(capabilitiesFetch.done({
            params: {},
            result
        }));
    }
}