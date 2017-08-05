import * as React from "react";

import CommentComponent from "./CommentComponent";
import {Comment, LineComments as LineCommentsState} from "../state/comments";
import {CommentForm} from "./CommentForm";

interface LineCommentsProps {
    canPostComment: boolean
    comments: LineCommentsState
    arrowOffset: number
    onSubmit: (text: string) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
}

export default class LineComments extends React.Component<LineCommentsProps, {}> {
    render() {
        const commentComps = this.props.comments.map((comment: Comment) => {
            return (<CommentComponent
                        key={comment.id} {...comment}
                        onResolve={this.props.onCommentResolve}
                        onUnresolve={this.props.onCommentUnresolve}/>);
        });

        return (
            <div>
                <div className="line-comments">
                    {commentComps}
                    { this.props.canPostComment && <CommentForm onSubmit={this.props.onSubmit}/> }
                </div>
                <div className="line-comments-arrow" style={{left: this.props.arrowOffset}}/>
            </div>
        )
    }
}
