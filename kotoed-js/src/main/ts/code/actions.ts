/**
 * Created by gagarski on 7/18/17.
 */

import actionCreatorFactory from 'typescript-fsa';
import {FileTreePath, nodePathToFilePath} from "./util/filetree";
import {fromJS, List} from "immutable";
import {File} from "./model"
import {CodeReviewState} from "./state";
import {waitTillReady, fetchRootDir, fetchFile} from "./data_fetch";

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

export function fetchRoot(payload: SubmissionPayload) {
    return (dispatch) => {
        dispatch(rootFetch.started({
            submissionId: payload.submissionId
        }));
        fetchRootDir(payload.submissionId).then((fileList) => {
            dispatch(rootFetch.done({
                params: {
                    submissionId: payload.submissionId
                },
                result: {
                    list: fileList
                }
            }))
        });
    }
}

export function fetchFileIfNeeded(payload: NodePathPayload & SubmissionPayload) {
    return (dispatch, getState: () => CodeReviewState) => {
        let filename = nodePathToFilePath(getState().fileTreeState.nodes, payload.treePath);
        dispatch(fileFetch.started({
            submissionId: payload.submissionId,
            filename
        }));  // Not used yet
        // TODO save file locally?
        fetchFile(3, filename).then(result => {  // TODO get submission id from URL
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