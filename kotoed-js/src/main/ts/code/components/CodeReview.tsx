import * as React from "react";
import {FileReviewProps, default as FileReview} from "./FileReview";
import FileTree, {FileTreeProps} from "./FileTree";
import {Comment} from "../model"
import {FileNodes, FileTreePath} from "../util/filetree";
import {FileComments} from "../state";
import {CmMode} from "../util/codemirror";

export interface CodeReviewProps {
    // TODO decompose this shit
    editorValue: string;
    editorMode: CmMode;
    filePath: string;
    nodePath: FileTreePath;
    editorComments: FileComments;
    fileTreeLoading: boolean;
    fileTreeNodes: FileNodes;
    onDirExpand: (path: number[]) => void;
    onDirCollapse: (path: number[]) => void;
    onFileSelect: (path: number[]) => void;
    onCommentSubmit: (file: string, line: number, text: string) => void
}

export default class CodeReview extends React.Component<CodeReviewProps> {
    render() {
        return (
            <div className="row code-review">
                <div className="col-md-3" style={{height: "100%", overflowY: "scroll"}}>
                    <FileTree nodes={this.props.fileTreeNodes}
                              onDirExpand={this.props.onDirExpand}
                              onDirCollapse={this.props.onDirCollapse}
                              onFileSelect={this.props.onFileSelect}
                              loading={this.props.fileTreeLoading}
                    />
                </div>
                <div className="col-md-9" style={{height: "100%"}}>
                    <FileReview value={this.props.editorValue}
                                mode={this.props.editorMode}
                                height="100%"
                                comments={this.props.editorComments}
                                filePath={this.props.filePath}
                                onSubmit={(line, text) => this.props.onCommentSubmit(this.props.filePath, line, text)}
                    />
                </div>
            </div>
        )
    }
}