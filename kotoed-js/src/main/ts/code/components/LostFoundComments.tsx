import * as React from "react"
import {Comment, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import CommentComponent from "./CommentComponent";
import {CommentList} from "./CommentList";
import {List} from "immutable";

interface LostFoundCommentsProps {
    comments: LostFoundCommentsState
    onExpand: (comments: List<Comment>) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
}

export class LostFoundComments extends React.Component<LostFoundCommentsProps, {}> {
    renderComments() {
        return this.props.comments.map((comment: Comment) => {
            return <CommentComponent
                key={comment.id}
                {...comment}
                onUnresolve={this.props.onCommentUnresolve}
                onResolve={this.props.onCommentResolve}
                notifyEditorAboutChange={() => {/* No editor to notify!*/} }
                onEdit={this.props.onEdit}/>
        }).toArray();
    }
    render() {
        return <div className="lost-found-comments">
            <CommentList {...this.props} notifyEditorAboutChange={() => {}}/>
        </div>
    }
}