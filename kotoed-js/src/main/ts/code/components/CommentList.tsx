import * as React from "react"

import {Comment} from "../state/comments";
import {List} from "immutable";
import CollapsedComments from "./CollapsedComments";
import CommentComponent from "./CommentComponent";
import {BaseCommentToRead} from "../../data/comment";
import {CommentTemplates} from "../remote/templates";

interface CommentListProps {
    comments: List<Comment>
    onCommentUnresolve: (id: number) => void
    onCommentResolve: (id: number) => void
    onEdit: (id: number, newText: string) => void
    onExpand: (comments: List<Comment>) => void
    notifyEditorAboutChange: () => void
    makeOriginalLink?: (comment: BaseCommentToRead) => string | undefined
    commentTemplates: CommentTemplates
}

export class CommentList extends React.Component<CommentListProps, {}> {
    renderNested = (): Array<JSX.Element> => {

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
            if (!comment.collapsed) {
                flushCollapsed();
                components.push(<CommentComponent
                    notifyEditorAboutChange={this.props.notifyEditorAboutChange}
                    key={comment.id} {...comment}
                    onResolve={lcProps.onCommentResolve}
                    onUnresolve={lcProps.onCommentUnresolve}
                    onEdit={this.props.onEdit}
                    makeOriginalLink={this.props.makeOriginalLink}
                    commentTemplates={this.props.commentTemplates}
                />);

            } else {
                collapsedAcc.push(comment);
            }
        });

        flushCollapsed();

        return components;
    };

    render() {
        return <div>
            {this.renderNested()}
        </div>
    }
}