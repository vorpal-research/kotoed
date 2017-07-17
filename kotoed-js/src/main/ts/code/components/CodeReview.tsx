import * as React from "react";
import {FileReviewProps, default as FileReview} from "./FileReview";
import FileTree from "./FileTree";

export default class CodeReview extends React.Component<FileReviewProps> {
    render() {
        return (
            <div className="row">
                <div className="col-md-4">
                    <FileTree/>
                </div>
                <div className="col-md-8">
                    <FileReview {...this.props}/>
                </div>
            </div>
        )
    }
}