import * as React from "react"
import {Comment, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import CommentComponent from "./CommentComponent";
import {CommentList} from "./CommentList";
import {List} from "immutable";
import ComponentWithLoading, {LoadingProperty} from "./ComponentWithLoading";

interface LostFoundCommentsProps {
    comments: LostFoundCommentsState
    onExpand: (comments: List<Comment>) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
    makeOriginalLink?: (submissionId: number, sourcefile: string, sourceline: number) => string | undefined
}

export class LostFoundComments extends ComponentWithLoading<LostFoundCommentsProps & LoadingProperty, {}> {

    render() {
        return <div>
            {this.renderVeil()}
            <div className="lost-found-comments">
                <CommentList {...this.props}
                             notifyEditorAboutChange={() => {}}
                             makeOriginalLink={this.props.makeOriginalLink}/>
            </div>
        </div>
    }
}