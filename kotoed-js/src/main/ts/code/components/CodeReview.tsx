import * as React from "react";

import FileReview from "./FileReview";
import FileTree from "./FileTree";
import {Comment, FileComments} from "../state/comments";
import {NodePath} from "../state/blueprintTree";
import {FileNode} from "../state/filetree";
import {CmMode} from "../util/codemirror";
import {List} from "immutable";

export interface CodeReviewProps {
    // TODO decompose this shit
    canPostComment: boolean
    editorValue: string;
    editorMode: CmMode;
    filePath: string;
    nodePath: NodePath;
    editorComments: FileComments;
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
    whoAmI: string
}

export default class CodeReview extends React.Component<CodeReviewProps> {
    render() {
        return (
            <div className="row code-review">
                <div className="col-md-3" style={{height: "100%", overflowY: "scroll"}}>
                    <FileTree root={this.props.root}
                              onDirExpand={this.props.onDirExpand}
                              onDirCollapse={this.props.onDirCollapse}
                              onFileSelect={this.props.onFileSelect}
                              loading={this.props.fileTreeLoading}
                    />
                </div>
                <div className="col-md-9" style={{height: "100%"}}>
                    <FileReview canPostComment={this.props.canPostComment}
                                value={this.props.editorValue}
                                mode={this.props.editorMode}
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
                </div>
            </div>
        )
    }
}