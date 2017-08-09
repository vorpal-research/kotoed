import * as React from "react";
import {Dispatch} from "redux";
import {connect} from "react-redux";


import CodeReview, {CodeReviewProps} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, editComment, expandHiddenComments, fileSelect, loadCode, loadLostFound, postComment,
    resetExpandedForLine,
    setCommentState,
    setCodePath, setLostFoundPath, resetExpandedForLostFound, unselectFile} from "../actions";
import {CodeReviewState} from "../state";
import {RouteComponentProps} from "react-router-dom";
import {FileComments} from "../state/comments";
import {push} from "react-router-redux";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "../remote/comments";
import {CODE_REVIEW_BASE_ADDR} from "../index";

interface OnRoute {
    onCodeRoute(submissionId: number, filename: string): void
    onLostFoundRoute(submissionId: number): void
}

interface CodeReviewUrl {
    submissionId: string
    path: string
}

export type RoutingCodeReviewProps = CodeReviewProps & RouteComponentProps<CodeReviewUrl> & OnRoute;

const mapStateToProps = function(store: CodeReviewState): Partial<RoutingCodeReviewProps> {
    return {
        editorComments: store.commentsState.comments.get(store.editorState.fileName, FileComments()),
        lostFoundComments: store.commentsState.lostFound,
        lostFoundAggregate: store.fileTreeState.lostFoundAggregate,
        editorValue: store.editorState.value,
        filePath: store.editorState.fileName,
        root: store.fileTreeState.root,
        nodePath: store.fileTreeState.selectedPath,
        canPostComment: store.capabilitiesState.capabilities.permissions.postComment,
        whoAmI: store.capabilitiesState.capabilities.principal.denizenId,
        editorLoading: store.editorState.loading || store.fileTreeState.loading || store.capabilitiesState.loading,
        fileTreeLoading: store.fileTreeState.loading || store.fileTreeState.aggregatesLoading || store.capabilitiesState.loading,
        lostFoundLoading: store.fileTreeState.aggregatesLoading || store.capabilitiesState.loading
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
            dispatch(setCodePath({
                treePath: path,
                submissionId: parseInt(ownProps.match.params.submissionId)
            }));
        },
        onLostFoundSelect: () => {
            dispatch(unselectFile());
            dispatch(resetExpandedForLostFound());
            dispatch(setLostFoundPath({
                submissionId: parseInt(ownProps.match.params.submissionId)
            }));
        },
        onCodeRoute: (submissionId, filename) => {
            dispatch(loadCode({
                submissionId,
                filename
            }));
        },
        onLostFoundRoute: (submissionId) => {
            dispatch(loadLostFound({
                submissionId
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

        onMarkerExpand: () => {
            dispatch(push(makeCodePath(parseInt(ownProps.match.params.submissionId), ownProps.match.params.path)));
        },

        onMarkerCollapse: (file, line) => {
            dispatch(resetExpandedForLine({
                file,
                line
            }));
            dispatch(push(makeCodePath(parseInt(ownProps.match.params.submissionId), ownProps.match.params.path)));
        },

        onCommentEdit: (file, line, commentId, newText) => {
            dispatch(editComment({
                commentId,
                newText
            }))
        },

        makeLastSeenLink: (submissionId, sourcefile, sourceline) => {
            if (sourcefile !== UNKNOWN_FILE && sourceline !== UNKNOWN_LINE)
                return `${CODE_REVIEW_BASE_ADDR}${makeCodePath(submissionId, sourcefile, sourceline)}`;
        }
    }
};

export const CODE_ROUTE_PATH = "/:submissionId(\\d+)/code/:path*";
export const LOST_FOUND_ROUTE_PATH = "/:submissionId(\\d+)/lost+found";

export function makeCodePath(submissionId: number, path: string, scrollTo?: number) {
    let hash = scrollTo !== undefined ? `#${scrollTo}` : "";
    return `/${submissionId}/code/${path}${hash}`
}

export function makeLostFoundPath(submissionId: number) {
    return `/${submissionId}/lost+found`
}

class RoutingContainer extends React.Component<RoutingCodeReviewProps> {
    // TODO this is fucked up
    private doRoute = () => {
        if (CODE_ROUTE_PATH === this.props.match.path) {
            let {submissionId, path} = this.props.match.params;
            this.props.onCodeRoute(parseInt(submissionId), path || "");
        } else if (LOST_FOUND_ROUTE_PATH === this.props.match.path) {
            let {submissionId} = this.props.match.params;
            this.props.onLostFoundRoute(parseInt(submissionId));
        }
    };

    componentDidMount() {
        this.doRoute()
    }

    componentDidUpdate(prevProps: RoutingCodeReviewProps) {
        if (prevProps.match.url !== this.props.match.url) {
            this.doRoute()
        }
    }

    getCodeReviewMode = () => {
        if (CODE_ROUTE_PATH === this.props.match.path) {
            return "code";
        } else {
            return "lost+found";
        }
    };

    // TODO this is SUPER fucked up
    getHash = (): number | undefined => {
        let hash = window.location.hash.split("#")[1];
        let hashInt = parseInt(hash);

        if (!isNaN(hashInt))
            return hashInt;

    };

    render() {
        return <CodeReview {...this.props} show={this.getCodeReviewMode()} scrollTo={this.getHash()}/>
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RoutingContainer);
