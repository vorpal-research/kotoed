import * as React from "react";
import * as moment from "moment";

import {Comment} from "../state";


type CommentProps = Comment

export default class CommentComponent extends React.Component<CommentProps, {}> {
    render() {
        return (
            <div className="panel panel-danger comment">
                <div className="panel-heading comment-heading">
                    {`${this.props.authorName}  @ ${moment(this.props.dateTime).format('LLLL')}`}
                </div>
                <div className="panel-body">
                    {`${this.props.text}`}
                </div>
            </div>
        );
    }
}