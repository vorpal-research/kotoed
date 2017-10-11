import * as React from "react";
import * as _ from "lodash"
import {Dispatch} from "redux";
import {connect} from "react-redux";
import * as QueryString from "query-string";

import CodeReview, {CodeReviewCallbacks, CodeReviewProps, CodeReviewPropsAndCallbacks} from "../components/CodeReview";
import {
    dirCollapse, dirExpand, editComment, expandHiddenComments, fileSelect, loadCode, loadLostFound, postComment,
    resetExpandedForLine,
    setCommentState,
    setCodePath, setLostFoundPath, resetExpandedForLostFound, unselectFile, emphasizeComment
} from "../actions";
import {CodeReviewState, ScrollTo} from "../state";
import {RouteComponentProps} from "react-router-dom";
import {FileComments} from "../state/comments";
import {push} from "react-router-redux";
import {Redirect} from "react-router";
import {makeCodeReviewCodePath, makeCommentPath} from "../../util/url";

interface OnRoute {
    onCodeRoute(submissionId: number, filename: string): void
    onLostFoundRoute(submissionId: number): void
}

interface CodeReviewUrl {
    submissionId: string
    path: string
}

export type RoutingCodeReviewProps = CodeReviewPropsAndCallbacks & RouteComponentProps<CodeReviewUrl> & OnRoute;

const mapStateToProps = function(store: CodeReviewState,
                                 ownProps: RouteComponentProps<CodeReviewUrl>): CodeReviewProps {
    return {
        submissionId: parseInt(ownProps.match.params.submissionId),
        submission: store.submissionState.submission,
        annotations: store.codeAnnotationsState.annotations,
        commentTemplates: store.commentTemplateState.templates,
        editor: {
            loading: store.editorState.loading || store.fileTreeState.loading || store.capabilitiesState.loading,
            value: store.editorState.value,
            file: store.editorState.fileName,
            comments: store.commentsState.comments.get(store.editorState.fileName, FileComments())
        },
        fileTree: {
            loading: store.fileTreeState.loading || store.fileTreeState.aggregatesLoading || store.capabilitiesState.loading,
            root: store.fileTreeState.root,
            path: store.fileTreeState.selectedPath
        },
        lostFound: {
            loading: store.fileTreeState.aggregatesLoading || store.capabilitiesState.loading,
            comments: store.commentsState.lostFound,
            aggregate: store.fileTreeState.lostFoundAggregate
        },
        capabilities: {
            canPostComment: store.capabilitiesState.capabilities.permissions.postComment,
            whoAmI: store.capabilitiesState.capabilities.principal.denizenId,
        },
        forms: {
            forms: store.formState
        }
    }
};

const mapDispatchToProps = function (dispatch: Dispatch<CodeReviewState>,
                                     ownProps: RouteComponentProps<CodeReviewUrl>): CodeReviewCallbacks & OnRoute {

    function removeHashFromUrl() {
        dispatch(push(makeCodeReviewCodePath(parseInt(ownProps.match.params.submissionId), ownProps.match.params.path)));
    }


    return {
        editor : {
            onMarkerExpand: () => {
                removeHashFromUrl();
            },
            onMarkerCollapse: (file, line) =>  {
                removeHashFromUrl();
                dispatch(resetExpandedForLine({
                    file,
                    line
                }));
            }
        },

        comments: {
            onCommentSubmit: (sourcefile, sourceline, text) => {
                dispatch(postComment({
                    submissionId: parseInt(ownProps.match.params.submissionId),
                    sourcefile,
                    sourceline,
                    text
                }))
            },

            onCommentResolve: (sourcefile, sourceline, id) => {
                dispatch(setCommentState({
                    sourcefile,
                    sourceline,
                    id,
                    state: "closed"
                }))
            },
            onCommentUnresolve: (sourcefile, sourceline, id) => {
                dispatch(setCommentState({
                    sourcefile,
                    sourceline,
                    id,
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

            onCommentEmphasize: (file, line, commentId) => {
                dispatch(emphasizeComment({
                    file,
                    line,
                    commentId
                }))
            },

            onCommentEdit: (sourcefile, sourceline, id, newText) => {
                dispatch(editComment({
                    sourcefile,
                    sourceline,
                    id,
                    text: newText
                }))
            },
            makeOriginalLink: makeCommentPath
        },

        fileTree: {
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
            }
        },

        lostFound: {
            onSelect: () => {
                dispatch(unselectFile());
                dispatch(resetExpandedForLostFound());
                dispatch(setLostFoundPath({
                    submissionId: parseInt(ownProps.match.params.submissionId)
                }));
            }
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
    }
};

export const CODE_ROUTE_PATH = "/submission/:submissionId(\\d+)/review/code/:path*";
export const LOST_FOUND_ROUTE_PATH = "/submission/:submissionId(\\d+)/review/lost+found";

export class RedirectToRoot extends React.Component<RouteComponentProps<CodeReviewUrl>> {
    render() {
        return <Redirect to={makeCodeReviewCodePath(parseInt(this.props.match.params.submissionId), "")}/>
    }
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
    getHash = (): ScrollTo => {
        // let hash = window.location.hash.split("#")[1];
        let data = QueryString.parse(window.location.hash);
        let line = parseInt(data.line) || undefined;
        let commentId = parseInt(data.commentId) || undefined;

        return {
            line,
            commentId
        }

    };

    render() {
        return <CodeReview {...this.props} scrollTo={this.getHash()} show={this.getCodeReviewMode()}/>
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps,
    (state, dispatch, own) => _.merge({}, state, dispatch, own)
)(RoutingContainer);
