import * as React from "react";
import {Dispatch} from "redux";
import {connect} from "react-redux";


import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, editComment, expandHiddenComments, fileSelect, initialize, postComment,
    resetExpandedForLine,
    setCommentState,
    setPath
} from "../actions";
import {CodeReviewState} from "../state";
import {RouteComponentProps} from "react-router-dom";
import {FileComments} from "../state/comments";

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
        editorComments: store.commentsState.comments.get(store.editorState.fileName, FileComments()),
        editorValue: store.editorState.value,
        filePath: store.editorState.fileName,
        root: store.fileTreeState.root,
        fileTreeLoading: store.fileTreeState.loading,
        nodePath: store.fileTreeState.selectedPath,
        canPostComment: store.capabilitiesState.capabilities.permissions.postComment,
        whoAmI: store.capabilitiesState.capabilities.principal.denizenId
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
        },

        onHiddenExpand: (file, line, comments) => {
            dispatch(expandHiddenComments({
                file,
                line,
                comments
            }))
        },

        onMarkerExpand: () => {},

        onMarkerCollapse: (file, line) => {
            dispatch(resetExpandedForLine({
                file,
                line
            }));
        },

        onCommentEdit: (file, line, commentId, newText) => {
            dispatch(editComment({
                commentId,
                newText
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
