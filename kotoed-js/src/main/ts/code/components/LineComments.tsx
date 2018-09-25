import * as React from "react";
import {fromJS, List} from "immutable";

import CommentComponent from "./CommentComponent";
import {Comment, LineComments as LineCommentsState} from "../state/comments";
import CommentForm from "./CommentForm";
import CollapsedComments from "./CollapsedComments";
import {CommentList} from "./CommentList";
import {BaseCommentToRead} from "../../data/comment";
import {FormState} from "../state/forms";
import {CommentTemplates} from "../remote/templates";

interface LineCommentsProps {
    canPostComment: boolean
    formState?: FormState
    comments: LineCommentsState
    commentTemplates: CommentTemplates
    arrowOffset: number
    onSubmit: (text: string) => void
    onCancel: () => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
    onExpand: (comments: List<Comment>) => void
    notifyEditorAboutChange: () => void
    makeOriginalLink?: (comment: BaseCommentToRead) => string | undefined
    whoAmI: string
}

export default class LineComments extends React.Component<LineCommentsProps, {}> {
    render() {

        return (
            <div>
                <div className="line-comments">
                    <CommentList {...this.props} />
                    {
                        this.props.canPostComment &&
                        <CommentForm
                            onSubmit={this.props.onSubmit}
                            onCancel={this.props.onCancel}
                            notifyEditorAboutChange={this.props.notifyEditorAboutChange}
                            whoAmI={this.props.whoAmI}
                            commentTemplates={this.props.commentTemplates}
                            formState={this.props.formState || {processing: false}}
                        />
                    }
                </div>
            </div>
        )
    }
}
