import * as React from "react";
import {FileReviewProps, default as FileReview} from "./FileReview";
import FileTree, {FileTreeNode, FileTreeProps, LoadingNode} from "./FileTree";
import {Comment} from "../model"

export class CodeReviewProps {
    // TODO decompose this shit
    editorValue: string;
    editorMode?: string;
    editorContentType?: string;
    editorHeight: number;
    editorComments: Comment[][];
    fileTreeNodes: Array<FileTreeNode | LoadingNode>;
    onDirExpand: (path: number[]) => void;
    onDirCollapse: (path: number[]) => void;
    onFileSelect: (path: number[]) => void;
}

export default class CodeReview extends React.Component<CodeReviewProps> {
    render() {
        return (
            <div className="row">
                <div className="col-md-2">
                    <FileTree nodes={this.props.fileTreeNodes}
                              onDirExpand={this.props.onDirExpand}
                              onDirCollapse={this.props.onDirCollapse}
                              onFileSelect={this.props.onFileSelect}
                    />
                </div>
                <div className="col-md-10">
                    <FileReview value={this.props.editorValue}
                                mode={this.props.editorMode}
                                contentType={this.props.editorContentType}
                                height={this.props.editorHeight}
                                comments={this.props.editorComments}
                    />
                </div>
            </div>
        )
    }
}