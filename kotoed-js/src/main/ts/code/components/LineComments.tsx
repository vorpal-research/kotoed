import * as React from "react";

import CommentComponent from "./CommentComponent";
import {Comment, LineCommentsState} from "../state";

interface LineCommentsProps {
    comments: LineCommentsState,
    canClose: boolean,
    arrowOffset: number
}

export default class LineComments extends React.Component<LineCommentsProps, {}> {
    render() {
        const commentComps = this.props.comments.map((comment: Comment) => {
            return (<CommentComponent key={comment.id} {...comment}/>);
        });

        return (
            <div>
                <div className="line-comments">
                    {commentComps}
                </div>
                <div className="line-comments-arrow" style={{left: this.props.arrowOffset}}/>
            </div>
        )
    }
}