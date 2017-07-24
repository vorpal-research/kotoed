import * as React from "react";
import {FileReviewProps, default as FileReview} from "./FileReview";
import FileTree, {FileTreeProps} from "./FileTree";
import {Comment} from "../model"
import {FileNodes} from "../util/filetree";

export class CodeReviewProps {
    // TODO decompose this shit
    editorValue: string;
    editorMode?: string;
    editorContentType?: string;
    editorHeight: number;
    editorComments: Comment[][];
    fileTreeLoading: boolean;
    fileTreeNodes: FileNodes;
    onDirExpand: (path: number[]) => void;
    onDirCollapse: (path: number[]) => void;
    onFileSelect: (path: number[]) => void;
}

export default class CodeReview extends React.Component<CodeReviewProps> {

    render() {
        return (
            <div className="row">
                <div className="col-md-3">
                    <FileTree nodes={this.props.fileTreeNodes}
                              onDirExpand={this.props.onDirExpand}
                              onDirCollapse={this.props.onDirCollapse}
                              onFileSelect={this.props.onFileSelect}
                              loading={this.props.fileTreeLoading}
                    />
                </div>
                <div className="col-md-9">
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