/**
 * Created by gagarski on 7/18/17.
 */

import actionCreatorFactory from 'typescript-fsa';
import {
    expandEverything, filePathToNodePath, FileTreePath, getNodeAt, nodePathToFilePath,
    selectFile
} from "./util/filetree";
import {File} from "./model"
import {CodeReviewState} from "./state";
import {waitTillReady, fetchRootDir, fetchFile} from "./data_fetch";
import {FileNotFoundError} from "./errors";
import {push} from "react-router-redux";
import {Dispatch} from "redux";
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
    value: string
}

export const dirExpand = actionCreator<NodePathPayload>('DIR_EXPAND');
export const dirCollapse = actionCreator<NodePathPayload>('DIR_COLLAPSE');
export const fileSelect = actionCreator<NodePathPayload>('FILE_SELECT');
export const dirFetch = actionCreator.async<NodePathPayload, DirFetchResult, {}>('DIR_FETCH');
export const rootFetch = actionCreator.async<SubmissionPayload, DirFetchResult, {}>('ROOT_FETCH');

export const fileFetch = actionCreator.async<FilePathPayload & SubmissionPayload, FileFetchResult, {}>('FILE_FETCH');


export function initialize(payload: SubmissionPayload & FilePathPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        dispatch(rootFetch.started({
            submissionId: payload.submissionId
        }));
        fetchRootDirIfNeeded(payload)(dispatch, getState).then(
            () => expandAndLoadIfNeeded(payload)(dispatch, getState)
        );
    }
}

export function fetchRootDirIfNeeded(payload: SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        if (!getState().fileTreeState.loading)
            return Promise.resolve();

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
                fetchFileIfNeeded({
                    treePath: numPath,
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

export function fetchFileIfNeeded(payload: NodePathPayload & SubmissionPayload) {
    return (dispatch: Dispatch<CodeReviewState>, getState: () => CodeReviewState) => {
        let filename = nodePathToFilePath(getState().fileTreeState.nodes, payload.treePath);
        dispatch(fileFetch.started({
            submissionId: payload.submissionId,
            filename
        }));  // Not used yet
        // TODO save file locally?
        fetchFile(payload.submissionId, filename).then(result => {
            dispatch(fileFetch.done({
                params: {
                    submissionId: payload.submissionId,
                    filename
                },
                result: {value: result}
            }));
        });

    }
}