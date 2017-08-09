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
    makeLastSeenLink?: (submissionId: number, sourcefile: string, sourceline: number) => string | undefined
}

export class LostFoundComments extends React.Component<LostFoundCommentsProps, {}> {

    render() {
        return <div className="lost-found-comments">
            <CommentList {...this.props}
                         notifyEditorAboutChange={() => {}}
                         makeLastSeenLink={this.props.makeLastSeenLink}/>
        </div>
    }
}