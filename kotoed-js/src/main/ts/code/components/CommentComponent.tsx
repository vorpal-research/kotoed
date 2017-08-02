import * as React from "react";
import * as ReactMarkdown from "react-markdown";
import * as moment from "moment";

import {Comment} from "../state";


type CommentProps = Comment

export default class CommentComponent extends React.Component<CommentProps, {}> {
    getPanelClass = () => {
        if (this.props.state == "open")
            return "panel-danger";
        else
            return "panel-default"
    };

    render() {
        return (
            <div className={`panel ${this.getPanelClass()} comment`}>
                <div className="panel-heading comment-heading">
                    {`${this.props.authorName}  @ ${moment(this.props.dateTime).format('LLLL')}`}
                </div>
                <div className="panel-body">
                    <ReactMarkdown source={this.props.text}/>
                </div>
            </div>
        );
    }
}