import * as React from "react";

import FileReview from "./FileReview";
import FileTree from "./FileTree";
import {Comment, FileComments, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import {NodePath} from "../state/blueprintTree";
import {FileNode} from "../state/filetree";
import {List} from "immutable";
import {RoutingCodeReviewProps} from "../containers/CodeReviewContainer";
import {LostFoundComments} from "./LostFoundComments";
import {CommentAggregate, UNKNOWN_FILE, UNKNOWN_LINE} from "../remote/comments";
import {makeSecondaryLabel} from "../util/filetree";

export interface CodeReviewProps {
    // TODO decompose this shit

    editorValue: string;
    filePath: string;
    nodePath: NodePath;
    editorComments: FileComments;
    lostFoundComments: LostFoundCommentsState
    lostFoundAggregate: CommentAggregate
    onLostFoundSelect: () => void
    show: "lost+found" | "code"

    fileTreeLoading: boolean;
    root: FileNode;

    onDirExpand: (path: number[]) => void;
    onDirCollapse: (path: number[]) => void;
    onFileSelect: (path: number[]) => void;
    onCommentSubmit: (file: string, line: number, text: string) => void
    onCommentUnresolve: (filePath: string, lineNumber: number, id: number) => void
    onCommentResolve: (filePath: string, lineNumber: number, id: number) => void
    onMarkerExpand: (file: string, lineNumber: number) => void
    onMarkerCollapse: (file: string, lineNumber: number) => void
    onHiddenExpand: (file: string, lineNumber: number, comments: List<Comment>) => void
    onCommentEdit: (file: string, line: number, id: number, newText: string) => void
    canPostComment: boolean
    whoAmI: string
}

export default class CodeReview extends React.Component<RoutingCodeReviewProps> {

    renderRightSide = () => {
        switch (this.props.show) {
            case "lost+found":
                return <LostFoundComments comments={this.props.lostFoundComments}
                                          onCommentUnresolve={(id) => this.props.onCommentUnresolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onCommentResolve={(id) => this.props.onCommentResolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onExpand={(comments) => this.props.onHiddenExpand(UNKNOWN_FILE, UNKNOWN_LINE, comments)}
                                          onEdit={(id, newText) => this.props.onCommentEdit(UNKNOWN_FILE, UNKNOWN_LINE, id, newText)}/>;
            case "code":
                return <FileReview canPostComment={this.props.canPostComment}
                                   value={this.props.editorValue}
                                   height="100%"
                                   comments={this.props.editorComments}
                                   filePath={this.props.filePath}
                                   onSubmit={(line, text) => this.props.onCommentSubmit(this.props.filePath, line, text)}
                                   onCommentResolve={this.props.onCommentResolve}
                                   onCommentUnresolve={this.props.onCommentUnresolve}
                                   onMarkerExpand={this.props.onMarkerExpand}
                                   onMarkerCollapse={this.props.onMarkerCollapse}
                                   onHiddenExpand={this.props.onHiddenExpand}
                                   onCommentEdit={this.props.onCommentEdit}
                                   whoAmI={this.props.whoAmI}
                />
        }
    };

    render() {
        return (
            <div className="row code-review">
                <div className="col-md-3" style={{height: "100%", overflowY: "scroll"}}>
                    <div className="code-review-tree-container">
                        <FileTree root={this.props.root}
                                  onDirExpand={this.props.onDirExpand}
                                  onDirCollapse={this.props.onDirCollapse}
                                  onFileSelect={this.props.onFileSelect}
                                  loading={this.props.fileTreeLoading}
                                  lostFoundAggregate={this.props.lostFoundAggregate}
                        />
                        <div className="lost-found-button-container">
                            <button className="btn btn-warning lost-found-button" onClick={this.props.onLostFoundSelect}>
                                Lost + Found {" "}
                                {makeSecondaryLabel(
                                    this.props.lostFoundAggregate
                                )}
                            </button>
                        </div>
                    </div>
                </div>
                <div className="col-md-9" style={{height: "100%", overflowY: "scroll"}}>
                    {this.renderRightSide()}
                </div>
            </div>
        )
    }
}