import * as React from "react";
import {guessCmMode} from "../util/codemirror";
import {Dispatch} from "redux";
import {connect} from "react-redux";


import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {dirCollapse, dirExpand, fileSelect, initialize, postComment, setCommentState, setPath} from "../actions";
import {CodeReviewState} from "../state";
import {RouteComponentProps} from "react-router-dom";

interface OnRoute {
    onRoute(submissionId: number, filename: string): void
}

interface CodeReviewUrl {
    submissionId: string
    path: string
}

type RoutingCodeReviewProps = CodeReviewProps & RouteComponentProps<CodeReviewUrl> & OnRoute;

const mapStateToProps = function(store: CodeReviewState): Partial<RoutingCodeReviewProps> {
    return {
        editorComments: store.editorState.displayedComments,
        editorValue: store.editorState.value,
        editorMode: store.editorState.mode,
        filePath: store.editorState.fileName,
        fileTreeNodes: store.fileTreeState.nodes,
        fileTreeLoading: store.fileTreeState.loading,
        nodePath: store.fileTreeState.selectedPath,
        canPostComment: store.capabilitiesState.capabilities.permissions.postComment
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
        },

        onCommentResolve: (sourcefile, sourceline, commentId) => {
            dispatch(setCommentState({
                commentId,
                state: "closed"
            }))
        },
        onCommentUnresolve: (sourcefile, sourceline, commentId) => {
            dispatch(setCommentState({
                commentId,
                state: "open"
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
