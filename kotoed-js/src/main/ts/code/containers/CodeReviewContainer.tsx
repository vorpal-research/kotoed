import * as React from "react";
import * as ReactRouter from "react-router"
import {CmMode, groupByLine, guessCmMode,} from "../util/codemirror";
import {codeJava, codeKt, codePlain, codeScala, comments} from "../data_stubs";
import {combineReducers, createStore, applyMiddleware} from "redux";
import {connect} from "react-redux";
import thunk from 'redux-thunk';

import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {toBlueprintTreeNodes} from "../util/filetree";
import {
    dirCollapse, dirExpand, fetchDirectoryIfNeeded, fetchFileIfNeeded, fileSelect
} from "../actions";
import {List} from "immutable";
import {editorReducer, fileTreeReducer} from "../reducers";

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer
    }),
    applyMiddleware(thunk)
);

const mapStateToProps = function(store): Partial<CodeReviewProps> {
    let {mode, contentType} = guessCmMode(store.editorState.fileName);
    return {
        editorHeight: 800,
        editorComments: groupByLine(comments),
        editorValue: store.editorState.value,
        editorMode: mode,
        editorContentType: contentType,
        fileTreeNodes: toBlueprintTreeNodes(store.fileTreeState.fileTree)
    }
};

const mapDispatchToProps = function (dispatch): Partial<CodeReviewProps> {
    return {
        onDirExpand: (path: number[]) => {
            dispatch(dirExpand({
                treePath: List(path)
            }));
            dispatch(fetchDirectoryIfNeeded({
                treePath: List(path)
            }))
        },
        onDirCollapse: (path: number[]) => {
            dispatch(dirCollapse({
                treePath: List(path)
            }));
        },
        onFileSelect: (path: number[]) => {
            dispatch(fileSelect({
                treePath: List(path)
            }));
            dispatch(fetchFileIfNeeded({
                treePath: List(path)
            }));
        },
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(CodeReview);