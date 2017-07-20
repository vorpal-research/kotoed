import * as React from "react";
import * as ReactRouter from "react-router"
import {CmMode, groupByLine, guessCmMode,} from "../util/codemirror";
import {comments} from "../data_stubs";
import {combineReducers, createStore, applyMiddleware} from "redux";
import {connect} from "react-redux";
import thunk from 'redux-thunk';

import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, fetchFileIfNeeded, fetchRoot, fileSelect
} from "../actions";
import {editorReducer, fileTreeReducer} from "../reducers";
import {CodeReviewState} from "../state";

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer
    }),
    applyMiddleware(thunk)
);

const mapStateToProps = function(store: CodeReviewState): Partial<CodeReviewProps> {
    let {mode, contentType} = guessCmMode(store.editorState.fileName);
    return {
        editorHeight: 800,
        editorComments: groupByLine(comments),
        editorValue: store.editorState.value,
        editorMode: mode,
        editorContentType: contentType,
        fileTreeNodes: store.fileTreeState.nodes,
        fileTreeLoading: store.fileTreeState.loading
    }
};

const mapDispatchToProps = function (dispatch, ownProps): Partial<CodeReviewProps> {
    return {
        onDirExpand: (path: number[]) => {
            dispatch(dirExpand({
                treePath: path
            }));
        },
        onDirCollapse: (path: number[]) => {
            dispatch(dirCollapse({
                treePath: path
            }));
        },
        onFileSelect: (path: number[]) => {
            dispatch(fileSelect({
                treePath: path
            }));
            dispatch(fetchFileIfNeeded({
                treePath: path,
                submissionId: parseInt(ownProps.match.params.submissionId)
            }));
        },
        onFileTreeMount: () => {
            dispatch(fetchRoot({
                submissionId: parseInt(ownProps.match.params.submissionId)
            }));
        }
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(CodeReview);