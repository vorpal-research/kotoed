import * as React from "react";
import {groupByLine, guessCmMode,} from "../util/codemirror";
import {combineReducers, createStore, applyMiddleware, Dispatch} from "redux";
import {connect} from "react-redux";
import thunk from 'redux-thunk';
import {routerMiddleware} from 'react-router-redux'
import {Map} from "immutable";

import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, fetchFileIfNeeded, fileSelect, initialize, postComment, setPath
} from "../actions";
import {CodeReviewState, FileComments, LineCommentsState} from "../state";
import {RouteComponentProps} from "react-router-dom";
import {nodePathToFilePath} from "../util/filetree";


interface OnRoute {
    onRoute(submissionId: number, filename: string): void
}

interface CodeReviewUrl {
    submissionId: string
    path: string
}

type RoutingCodeReviewProps = CodeReviewProps & RouteComponentProps<CodeReviewUrl> & OnRoute;

const mapStateToProps = function(store: CodeReviewState): Partial<RoutingCodeReviewProps> {
    let {mode, contentType} = guessCmMode(store.editorState.fileName);
    return {
        editorComments: (store.comments || Map<string, FileComments>()).
            get(store.editorState.fileName, Map<number, LineCommentsState>()),
        editorValue: store.editorState.value,
        editorMode: mode,
        editorContentType: contentType,
        fileTreeNodes: store.fileTreeState.nodes,
        fileTreeLoading: store.fileTreeState.loading,
        filePath: nodePathToFilePath(store.fileTreeState.nodes, store.fileTreeState.selectedPath),
        nodePath: store.fileTreeState.selectedPath
    }
};

const mapDispatchToProps = function (dispatch: Dispatch<CodeReviewState>,
                                     ownProps: RouteComponentProps<CodeReviewUrl>): Partial<RoutingCodeReviewProps> {
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
            dispatch(setPath({
                treePath: path,
                submissionId: parseInt(ownProps.match.params.submissionId)
            }));
        },
        onRoute: (submissionId, filename) => {
            dispatch(initialize({
                submissionId,
                filename
            }));
        },
        onCommentSubmit: (sourcefile, sourceline, text) => {
            dispatch(postComment({
                submissionId: parseInt(ownProps.match.params.submissionId),
                sourcefile,
                sourceline,
                text
            }))
        }
    }
};

class RoutingContainer extends React.Component<RoutingCodeReviewProps> {
    componentDidMount() {
        let {submissionId, path} = this.props.match.params;
        this.props.onRoute(parseInt(submissionId), path || "");
    }

    componentDidUpdate(prevProps: RoutingCodeReviewProps) {
        let {submissionId: prevSubmissionId, path: prevPath} = prevProps.match.params;
        let {submissionId, path} = this.props.match.params;

        if (prevSubmissionId !== submissionId || prevPath !== path) {
            this.props.onRoute(parseInt(submissionId), path  || "");
        }
    }

    render() {
        return <CodeReview {...this.props}/>
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RoutingContainer);
