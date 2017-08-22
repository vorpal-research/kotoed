import * as React from "react"
import {Comment, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import {CommentList} from "./CommentList";
import {List} from "immutable";
import {ScrollTo} from "../state/index";
import {BaseCommentToRead} from "../remote/comments";
import ComponentWithLoading, {LoadingProperty} from "../../views/components/ComponentWithLoading";

interface LostFoundCommentsProps {
    comments: LostFoundCommentsState
    onExpand: (comments: List<Comment>) => void
    onCommentEmphasize: (commentId: number) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
    makeOriginalLink?: (comment: BaseCommentToRead) => string | undefined
    scrollTo: ScrollTo
}

export class LostFoundComments extends ComponentWithLoading<LostFoundCommentsProps & LoadingProperty, {}> {

    private scrollTo = () => {
        if (this.props.scrollTo === undefined)
            return;

        let {commentId} = this.props.scrollTo;

        // TODO replace collapsing with scrolling
        if (commentId === undefined)
            return;

        this.props.onCommentEmphasize(
            commentId
        )


    };

    componentDidMount() {
        this.scrollTo()
    }

    componentDidUpdate(oldProps: LostFoundCommentsProps) {
        if (oldProps.scrollTo.commentId !== this.props.scrollTo.commentId)
            this.scrollTo();

    }

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