import {List} from "immutable";
import * as React from "react";

import {Comment} from "../state/comments";

interface CollapsedCommentsProps {
    comments: List<Comment>
    onClick: (comments: List<Comment>) => void
}

export default class CollapsedComments extends React.Component<CollapsedCommentsProps, {}> {
    pluralizeComments = () => {
        return this.props.comments.size > 1 ? "comments" : "comment";
    };

    render() {
        return <div className="hidden-comments-heading" onClick={() => this.props.onClick(this.props.comments)}>
            {`${this.props.comments.size} collapsed ${this.pluralizeComments()}`}
        </div>
    }
}