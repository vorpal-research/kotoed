import {List, Map} from "immutable";


import actionCreatorFactory from 'typescript-fsa';
import {
    CodeReviewState
} from "./state";
import {
    Comment, CommentsState, CommentState, FileComments, LineComments, ReviewComments
} from "./state/comments";
import {
    DiffBase,
    fetchDiff,
    fetchFile,
    fetchRootDir,
    File,
    FileDiffResponse,
    FileDiffResult,
    FileType, updateDiffPreference
} from "./remote/code";
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
    editComment as doEditComment
} from "./remote/comments";
import {Capabilities, fetchCapabilities} from "./remote/capabilities";
import {applyDiffToFileTree, getFilePath, getNodePath} from "./util/filetree";
import {NodePath} from "./state/blueprintTree";
import {makeCodeReviewCodePath, makeCodeReviewLostFoundPath} from "../util/url";
import {DbRecordWrapper, isStatusFinal} from "../data/verification";
import {SubmissionToRead} from "../data/submission";
import {pollDespairing} from "../util/poll";
import {fetchSubmission} from "../submissionDetails/remote";
import {fetchAnnotations} from "./remote/annotations";
import {ReviewAnnotations} from "./state/annotations";
import {CommentTemplates, fetchCommentTemplates} from "./remote/templates";
import natsort from "natsort";
import {pick, typedKeys} from "../util/common";

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
    id: number,
    sourcefile: string
    sourceline: number
    state: CommentState
}

interface CommentEditPayload {
    id: number,
    sourcefile: string
    sourceline: number
    text: string
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

interface CommentEmphasizePayload {
    file: string
    line: number
    commentId: number
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

interface FormLockUnlockPayload {
    sourcefile: string
    sourceline: number
}

interface DiffBasePayload {
    diffBase: DiffBase
}

interface PersistPayload {
    persist: boolean
}

interface DiffResultPayload {
    diff: FileDiffResult[]
}

// Local actions
export const dirExpand = actionCreator<NodePathPayload>('DIR_EXPAND');
export const dirCollapse = actionCreator<NodePathPayload>('DIR_COLLAPSE');
export const fileSelect = actionCreator<NodePathPayload>('FILE_SELECT');
export const fileUnselect = actionCreator<{}>('FILE_UNSELECT');
export const aggregatesUpdate = actionCreator<AggregatesUpdatePayload>("AGGREGATES_UPDATE");
export const hiddenCommentsExpand = actionCreator<HiddenCommentsExpandPayload>("HIDDEN_COMMENTS_EXPAND");
export const commentEmphasize = actionCreator<CommentEmphasizePayload>("COMMENT_EMPHASIZE");

export const expandedResetForLine = actionCreator<ExpandedResetForLinePayload>("EXPANDED_RESET_FOR_LINE");
export const expandedResetForFile = actionCreator<ExpandedResetForFilePayload>("EXPANDED_RESET_FOR_FILE");
export const expandedResetForLostFound = actionCreator<{}>("EXPANDED_RESET_FOR_LOST_FOUND");

//Submission fetch actions
export const submissionFetch = actionCreator.async<SubmissionPayload, DbRecordWrapper<SubmissionToRead>, {}>('SUBMISSION_FETCH');


// File or dir fetch actions
export const rootFetch = actionCreator.async<SubmissionPayload, DirFetchResult & DiffResultPayload, {}>('ROOT_FETCH');
export const fileLoad = actionCreator.async<FilePathPayload & SubmissionPayload, FileFetchResult, {}>('FILE_LOAD');
export const diffFetch = actionCreator.async<SubmissionPayload & DiffBasePayload, FileDiffResponse>('DIFF_FETCH')

// Annotation fetch actions
export const annotationsFetch = actionCreator.async<number, ReviewAnnotations, {}>('ANNOTATION_FETCH');

// Template fetch actions
export const commentTemplateFetch = actionCreator.async<{}, CommentTemplates, {}>('COMMENT_TEMPLATE_FETCH');

// Comment fetch actions
export const commentsFetch = actionCreator.async<SubmissionPayload, CommentsState, {}>('COMMENT_FETCH');
export const commentAggregatesFetch =
    actionCreator.async<SubmissionPayload, CommentAggregates, {}>("COMMENT_AGGREGATES_FETCH");
export const commentPost = actionCreator.async<PostCommentPayload, Comment, {}>('COMMENT_POST');
export const commentStateUpdate = actionCreator.async<CommentStatePayload, CommentStatePayload>('COMMENT_STATE_UPDATE');
export const commentEdit = actionCreator.async<CommentEditPayload, CommentEditPayload>('COMMENT_EDIT');


// Capabilities
export const capabilitiesFetch = actionCreator.async<{}, Capabilities, {}>('CAPABILITIES_FETCH');


export function pollSubmissionIfNeeded(payload: SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {

        if (getState().submissionState.submission)
            return;

        function dispatchDone(res: DbRecordWrapper<SubmissionToRead>) {
            dispatch(submissionFetch.done({
                params: payload,
                result: res
            }));
        }

        await pollDespairing({
            action: () => fetchSubmission(payload.submissionId),
            isGoodEnough: (sub) => isStatusFinal(sub.verificationData.status),
            onIntermediate: dispatchDone,
            onFinal: dispatchDone,
            onGiveUp: dispatchDone
        });
    }
}

export function loadCode(payload: SubmissionPayload & FilePathPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {
        await pollSubmissionIfNeeded(payload)(dispatch, getState);
        let sub = getState().submissionState.submission;
        if (!sub || sub.verificationData.status !== "Processed")
            return;

        await fetchCapabilitiesIfNeeded(payload)(dispatch, getState);
        await fetchRootDirIfNeeded(payload)(dispatch, getState);
        await fetchCommentsIfNeeded(payload)(dispatch, getState);
        await fetchAnnotationsIfNeeded(payload.submissionId)(dispatch, getState);
        await fetchCommentTemplatesIfNeeded()(dispatch, getState);
        await expandAndLoadIfNeeded(payload)(dispatch, getState);
        await fetchCommentAggregatesIfNeeded(payload)(dispatch, getState);
    }
}

export function loadLostFound(payload: SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState): Promise<void> => {
        await pollSubmissionIfNeeded(payload);
        let sub = getState().submissionState.submission;
        if (!sub || sub.verificationData.status !== "Processed")
            return;
        await fetchCapabilitiesIfNeeded(payload)(dispatch, getState);
        await fetchRootDirIfNeeded(payload)(dispatch, getState);
        await fetchCommentsIfNeeded(payload)(dispatch, getState);
        await fetchAnnotationsIfNeeded(payload.submissionId)(dispatch, getState);
        await fetchCommentTemplatesIfNeeded()(dispatch, getState);
        await fetchCommentAggregatesIfNeeded(payload)(dispatch, getState);
        dispatch(fileUnselect({}));
    }
}

// Lower numbers are displayed first
const fileTypeDisplayOrder = {directory: 1, file: 2}
const naturalSorter = natsort()
const typeSorter = (a: File, b: File) => fileTypeDisplayOrder[a.type] - fileTypeDisplayOrder[b.type]

export function fetchRootDirIfNeeded(payload: SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        const state = getState();
        if (!state.fileTreeState.loading)
            return Promise.resolve();

        dispatch(rootFetch.started({
            submissionId: payload.submissionId
        }));

        const root = await fetchRootDir(payload.submissionId);

        const recursiveSorter = (node: File) => {
            if (node.children == null) {
                return
            }
            node.children.sort((a: File, b: File) =>
                typeSorter(a, b) || naturalSorter(a.name, b.name)
            )
            for (let child of node.children) {
                recursiveSorter(child)
            }
        }
        recursiveSorter(root)

        const diff = await fetchDiff(payload.submissionId, state.diffState.base);

        dispatch(rootFetch.done({
            params: {
                submissionId: payload.submissionId
            },
            result: {
                root,
                diff: diff.diff
            }
        }))
        dispatch(diffFetch.done({
            params: {
                submissionId: payload.submissionId,
                diffBase: state.diffState.base
            },
            result: diff
        }))
    };
}

export function expandAndLoadIfNeeded(payload: SubmissionPayload & FilePathPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let numPath: NodePath;
        try {
            numPath = getNodePath(getState().fileTreeState.root, payload.filename);
        } catch (e) {
            numPath = [];
            console.warn(e);
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
        dispatch(push(makeCodeReviewCodePath(payload.submissionId, filename)))
    }
}

export function setLostFoundPath(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(push(makeCodeReviewLostFoundPath(payload.submissionId)))
    }
}


export function loadFileToEditor(payload: FilePathPayload & SubmissionPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let {filename} = payload;
        const state = getState();
        dispatch(fileLoad.started({
            submissionId: payload.submissionId,
            filename
        }));  // Not used yet
        // TODO save file locally?

        resetExpandedForFile({
            file: filename
        })(dispatch, getState);

        const result = await fetchFile(payload.submissionId, filename);
        dispatch(fileLoad.done({
            params: {
                submissionId: payload.submissionId,
                filename
            },
            result: {
                value: result,
                displayedComments: state.commentsState.comments.get(filename, Map<number, LineComments>()),
            }
        }));

    }
}

export function updateDiff(payload: SubmissionPayload & DiffBasePayload & PersistPayload) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(diffFetch.started(payload))

        const state = getState();
        const patchedType = payload.diffBase.type == "SUBMISSION_ID" ? "PREVIOUS_CLOSED" : payload.diffBase.type;

        if (payload.persist) {
            updateDiffPreference(state.capabilitiesState.capabilities.principal, patchedType)
        }

        const diff = await fetchDiff(payload.submissionId, payload.diffBase);

        dispatch(diffFetch.done({
            params: payload,
            result: diff
        }))
    }
}

export function fetchAnnotationsIfNeeded(payload: number) {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().codeAnnotationsState.loading)
            return;

        dispatch(annotationsFetch.started(payload));  // Not used yet

        let result = await fetchAnnotations(payload);

        dispatch(annotationsFetch.done({
            params: payload,
            result: result
        }));
    }
}

export function fetchCommentTemplatesIfNeeded() {
    return async (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().commentTemplateState.loading)
            return;

        dispatch(commentTemplateFetch.started({}));

        let result = await fetchCommentTemplates();

        dispatch(commentTemplateFetch.done({
            params: {},
            result: result
        }));
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
        dispatch(commentPost.started(payload));
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

        let result = await doSetCommentState(payload.id, payload.state);

        dispatch(commentStateUpdate.done({
            params: payload,
            result: result
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

        let result = await doEditComment(payload.id, payload.text);

        dispatch(commentEdit.done({
            params: payload,
            result: result
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

export function emphasizeComment(payload: CommentEmphasizePayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(commentEmphasize(payload));
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
