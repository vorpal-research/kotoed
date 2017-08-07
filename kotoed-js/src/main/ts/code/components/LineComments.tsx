import * as React from "react";
import {fromJS, List} from "immutable";

import CommentComponent from "./CommentComponent";
import {Comment, LineComments as LineCommentsState} from "../state/comments";
import CommentForm from "./CommentForm";
import CollapsedComments from "./CollapsedComments";

interface LineCommentsProps {
    canPostComment: boolean
    comments: LineCommentsState
    arrowOffset: number
    onSubmit: (text: string) => void
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onExpand: (comments: List<Comment>) => void
    notifyEditorAboutChange: () => void
    onEdit: (id: number, newText: string) => void
    whoAmI: string
}

export default class LineComments extends React.Component<LineCommentsProps, {}> {

    getNested = (): Array<JSX.Element> => {

        let collapsedAcc: Array<Comment> = [];
        let lcProps = this.props;
        let components: Array<JSX.Element> = []; // TODO generators maybe?

        const flushCollapsed = () => {
            if (collapsedAcc.length !== 0) {
                components.push(
                    <CollapsedComments
                        key={collapsedAcc[0].id /* Strange but good enough */}
                        comments={List<Comment>(collapsedAcc)}
                        onClick={this.props.onExpand}/>);
                collapsedAcc = []
            }
        };

        this.props.comments.forEach((comment: Comment) => {
            if (comment.state === "open" || !comment.collapsed) {
                flushCollapsed();
                components.push(<CommentComponent
                    notifyEditorAboutChange={this.props.notifyEditorAboutChange}
                    key={comment.id} {...comment}
                    onResolve={lcProps.onCommentResolve}
                    onUnresolve={lcProps.onCommentUnresolve}
                    onEdit={this.props.onEdit}/>);

            } else {
                collapsedAcc.push(comment);
            }
        });

        flushCollapsed();

        return components;
    };

    render() {

        return (
            <div>
                <div className="line-comments">
                    {this.getNested()}
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
