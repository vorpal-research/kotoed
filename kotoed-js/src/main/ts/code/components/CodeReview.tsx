import * as React from "react";

import FileReview from "./FileReview";
import FileTree from "./FileTree";
import {Comment, FileComments, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import {NodePath} from "../state/blueprintTree";
import {FileNode} from "../state/filetree";
import {List} from "immutable";
import {LostFoundComments} from "./LostFoundComments";
import {CommentAggregate, UNKNOWN_FILE, UNKNOWN_LINE} from "../remote/comments";
import {makeSecondaryLabel} from "../util/filetree";
import SpinnerWithVeil from "./SpinnerWithVeil";

export interface CodeReviewProps {
    submissionId: number
    editor: {
        loading: boolean
        value: string
        file: string
        comments: FileComments
        scrollTo?: number
    }

    fileTree: {
        loading: boolean
        path: NodePath
        root: FileNode
    }

    lostFound: {
        loading: boolean
        comments: LostFoundCommentsState
        aggregate: CommentAggregate
    }

    capabilities: {
        canPostComment: boolean
        whoAmI: string
    }
}

interface CodeReviewPropsFromRouting {
    show: "lost+found" | "code"
}

export interface CodeReviewCallbacks {
    editor : {
        onMarkerExpand: (file: string, lineNumber: number) => void
        onMarkerCollapse: (file: string, lineNumber: number) => void
    }

    comments: {
        onCommentSubmit: (file: string, line: number, text: string) => void
        onCommentUnresolve: (filePath: string, lineNumber: number, id: number) => void
        onCommentResolve: (filePath: string, lineNumber: number, id: number) => void
        onHiddenExpand: (file: string, lineNumber: number, comments: List<Comment>) => void
        onCommentEdit: (file: string, line: number, id: number, newText: string) => void
        makeOriginalLink?: (submissionId: number, sourcefile: string, sourceline: number) => string | undefined
    }

    fileTree: {
        onDirExpand: (path: number[]) => void;
        onDirCollapse: (path: number[]) => void;
        onFileSelect: (path: number[]) => void;
    }

    lostFound: {
        onSelect: () => void

    }
}

export type CodeReviewPropsAndCallbacks = CodeReviewProps & CodeReviewCallbacks

export default class CodeReview extends React.Component<CodeReviewPropsAndCallbacks & CodeReviewPropsFromRouting> {

    makeOriginalLinkOrUndefined = (submissionId: number, sourcefile: string, sourceline: number) => {
        if (this.props.comments.makeOriginalLink && submissionId !== this.props.submissionId)
            return this.props.comments.makeOriginalLink(submissionId, sourcefile, sourceline)
    };

    renderRightSide = () => {
        switch (this.props.show) {
            case "lost+found":
                return <LostFoundComments comments={this.props.lostFound.comments}
                                          onCommentUnresolve={(id) => this.props.comments.onCommentUnresolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onCommentResolve={(id) => this.props.comments.onCommentResolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onExpand={(comments) => this.props.comments.onHiddenExpand(UNKNOWN_FILE, UNKNOWN_LINE, comments)}
                                          onEdit={(id, newText) => this.props.comments.onCommentEdit(UNKNOWN_FILE, UNKNOWN_LINE, id, newText)}
                                          makeOriginalLink={this.props.comments.makeOriginalLink}
                                          loading={this.props.lostFound.loading}
                />;
            case "code":
                if (this.props.editor.file !== "")
                    return <FileReview canPostComment={this.props.capabilities.canPostComment}
                                       value={this.props.editor.value}
                                       height="100%"
                                       comments={this.props.editor.comments}
                                       filePath={this.props.editor.file}
                                       onSubmit={(line, text) => this.props.comments.onCommentSubmit(this.props.editor.file, line, text)}
                                       onCommentResolve={this.props.comments.onCommentResolve}
                                       onCommentUnresolve={this.props.comments.onCommentUnresolve}
                                       onMarkerExpand={this.props.editor.onMarkerExpand}
                                       onMarkerCollapse={this.props.editor.onMarkerCollapse}
                                       onHiddenExpand={this.props.comments.onHiddenExpand}
                                       onCommentEdit={this.props.comments.onCommentEdit}
                                       whoAmI={this.props.capabilities.whoAmI}
                                       scrollTo={this.props.editor.scrollTo}
                                       loading={this.props.editor.loading}
                                       makeOriginalCommentLink={this.makeOriginalLinkOrUndefined}
                    />;
                else
                    return <div className="no-file-chosen"><div>Please choose file</div></div>
        }
    };

    renderFileTreeVeil = () => {
        if (this.props.fileTree.loading)
            return <SpinnerWithVeil/>;
        else
            return null;
    };

    render() {
        return (
            <div className="row code-review">
                <div className="col-md-3" id="code-review-left">
                    {this.renderFileTreeVeil()}
                    <div className="code-review-tree-container">
                        <FileTree root={this.props.fileTree.root}
                                  onDirExpand={this.props.fileTree.onDirExpand}
                                  onDirCollapse={this.props.fileTree.onDirCollapse}
                                  onFileSelect={this.props.fileTree.onFileSelect}
                                  loading={this.props.fileTree.loading}
                                  lostFoundAggregate={this.props.lostFound.aggregate}
                        />
                        <div className="lost-found-button-container">
                            <button className="btn btn-warning lost-found-button" onClick={this.props.lostFound.onSelect}>
                                Lost + Found {" "}
                                {makeSecondaryLabel(
                                    this.props.lostFound.aggregate
                                )}
                            </button>
                        </div>
                    </div>
                </div>
                <div className="col-md-9" id="code-review-right">
                    {this.renderRightSide()}
                </div>
            </div>
        )
    }
}