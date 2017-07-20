/**
 * Created by gagarski on 7/18/17.
 */

import actionCreatorFactory from 'typescript-fsa';
import {FileTreePath, nodePathToFilePath, nodePathToStoragePath} from "./util/filetree";
import {fileToStoredFile, StoredFile} from "./model";
import {fromJS, List} from "immutable";
import {fetchFile, listDir} from "./data_stubs";

const actionCreator = actionCreatorFactory();

interface NodePathPayload {
    treePath: FileTreePath
}

interface FilePathPayload {
    filename: string
}



interface DirFetchResult {
    list: List<StoredFile>
}

interface FileFetchResult {
    value: string
}

export const dirExpand = actionCreator<NodePathPayload>('DIR_EXPAND');
export const dirCollapse = actionCreator<NodePathPayload>('DIR_COLLAPSE');
export const fileSelect = actionCreator<NodePathPayload>('FILE_SELECT');
export const dirFetch = actionCreator.async<NodePathPayload, DirFetchResult, {}>('DIR_FETCH');
export const fileFetch = actionCreator.async<FilePathPayload, FileFetchResult, {}>('FILE_FETCH');

export function fetchDirectoryIfNeeded(payload: NodePathPayload) {
    return (dispatch, getState) => {
        if (getState().fileTreeState.fileTree.getIn(nodePathToStoragePath(payload.treePath).concat("isLoaded")))
            return; // Nothing to do

        dispatch(dirFetch.started(payload));  // Not used yet

        listDir(nodePathToFilePath(getState().fileTreeState.fileTree, payload.treePath)).then(result => {
            let newChildren = fromJS(result.map(fileToStoredFile));
            dispatch(dirFetch.done({
                params: payload,
                result: {
                    list: newChildren
                }
            }))
        });

    }
}

export function fetchFileIfNeeded(payload: NodePathPayload) {
    return (dispatch, getState) => {
        let filename = nodePathToFilePath(getState().fileTreeState.fileTree, payload.treePath);
        dispatch(fileFetch.started({filename}));  // Not used yet

        fetchFile(filename).then(result => {
            dispatch(fileFetch.done({
                params: {filename},
                result: {value: result}
            }));
        });

    }
}