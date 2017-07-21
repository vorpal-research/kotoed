import * as React from "react";
import {groupByLine, guessCmMode,} from "../util/codemirror";
import {comments} from "../data_stubs";
import {combineReducers, createStore, applyMiddleware} from "redux";
import {connect} from "react-redux";
import thunk from 'redux-thunk';
import {routerMiddleware, push} from 'react-router-redux'

import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, fetchFileIfNeeded, fileSelect, initialize
} from "../actions";
import {editorReducer, fileTreeReducer} from "../reducers";
import {CodeReviewState} from "../state";
import createHistory from 'history/createBrowserHistory'

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer
    }),
    applyMiddleware(thunk),
    applyMiddleware(routerMiddleware(createHistory()))
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
        onMount: () => {
            dispatch(initialize({
                submissionId: parseInt(ownProps.match.params.submissionId),
                filename: ownProps.match.params.path || ""
            }));
        }
    }
};

export default connect(mapStateToProps, mapDispatchToProps)(CodeReview);