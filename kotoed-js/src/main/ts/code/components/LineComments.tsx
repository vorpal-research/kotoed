import * as React from "react";
import {fromJS, List} from "immutable";

import CommentComponent from "./CommentComponent";
import {Comment, LineComments as LineCommentsState} from "../state/comments";
import CommentForm from "./CommentForm";
import CollapsedComments from "./CollapsedComments";
import {CommentList} from "./CommentList";

interface LineCommentsProps {
    canPostComment: boolean
    comments: LineCommentsState
    arrowOffset: number
    onSubmit: (text: string) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
    onExpand: (comments: List<Comment>) => void
    notifyEditorAboutChange: () => void
    makeOriginalLink?: (submissionId: number, sourcefile: string, sourceline: number) => string | undefined
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
                            notifyEditorAboutChange={this.props.notifyEditorAboutChange}
                            whoAmI={this.props.whoAmI}
                        />
                    }
                </div>
                <div className="line-comments-arrow" style={{left: this.props.arrowOffset}}/>
            </div>
        )
    }
}
