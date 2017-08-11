import * as React from "react";
import * as _ from "lodash"

import {Dispatch} from "redux";
import {connect} from "react-redux";


import CodeReview, {CodeReviewCallbacks, CodeReviewProps, CodeReviewPropsAndCallbacks} from "../components/CodeReview";
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
import {Redirect} from "react-router";

interface OnRoute {
    onCodeRoute(submissionId: number, filename: string): void
    onLostFoundRoute(submissionId: number): void
}

interface CodeReviewUrl {
    submissionId: string
    path: string
}

export type RoutingCodeReviewProps = CodeReviewPropsAndCallbacks & RouteComponentProps<CodeReviewUrl> & OnRoute;

const mapStateToProps = function(store: CodeReviewState): CodeReviewProps {
    return {
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
        }
    }
};

const mapDispatchToProps = function (dispatch: Dispatch<CodeReviewState>,
                                     ownProps: RouteComponentProps<CodeReviewUrl>): CodeReviewCallbacks & OnRoute {
    function removeHashFromUrl() {
        dispatch(push(makeCodePath(parseInt(ownProps.match.params.submissionId), ownProps.match.params.path)));
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
                removeHashFromUrl();
                dispatch(postComment({
                    submissionId: parseInt(ownProps.match.params.submissionId),
                    sourcefile,
                    sourceline,
                    text
                }))
            },

            onCommentResolve: (sourcefile, sourceline, commentId) => {
                removeHashFromUrl();
                dispatch(setCommentState({
                    commentId,
                    state: "closed"
                }))
            },
            onCommentUnresolve: (sourcefile, sourceline, commentId) => {
                removeHashFromUrl();
                dispatch(setCommentState({
                    commentId,
                    state: "open"
                }))
            },

            onHiddenExpand: (file, line, comments) => {
                removeHashFromUrl();
                dispatch(expandHiddenComments({
                    file,
                    line,
                    comments
                }))
            },
            onCommentEdit: (file, line, commentId, newText) => {
                removeHashFromUrl();
                dispatch(editComment({
                    commentId,
                    newText
                }))
            }
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
            },
            makeLastSeenLink: (submissionId, sourcefile, sourceline) => {
                if (sourcefile !== UNKNOWN_FILE && sourceline !== UNKNOWN_LINE)
                    return `${CODE_REVIEW_BASE_ADDR}${makeCodePath(submissionId, sourcefile, sourceline)}`;
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

export const CODE_ROUTE_PATH = "/:submissionId(\\d+)/code/:path*";
export const LOST_FOUND_ROUTE_PATH = "/:submissionId(\\d+)/lost+found";

export function makeCodePath(submissionId: number, path: string, scrollTo?: number) {
    let hash = scrollTo !== undefined ? `#${scrollTo}` : "";
    return `/${submissionId}/code/${path}${hash}`
}

export function makeLostFoundPath(submissionId: number) {
    return `/${submissionId}/lost+found`
}

export class RedirectToRoot extends React.Component<RouteComponentProps<CodeReviewUrl>> {
    render() {
        return <Redirect to={makeCodePath(parseInt(this.props.match.params.submissionId), "")}/>
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
    getHash = (): number | undefined => {
        let hash = window.location.hash.split("#")[1];
        let hashInt = parseInt(hash);

        if (!isNaN(hashInt))
            return hashInt;

    };

    render() {
        let crProps = {...this.props};
        let editorProps = {...crProps.editor};
        editorProps.scrollTo = this.getHash();
        crProps.editor = editorProps;
        return <CodeReview {...crProps} show={this.getCodeReviewMode()}/>
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps,
    (state, dispatch, own) => _.merge({}, state, dispatch, own)
)(RoutingContainer);
