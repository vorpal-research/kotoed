import * as React from "react";

import {Comment} from "../model"
import CommentComponent from "./CommentComponent";

interface LineCommentsProps {
    comments: Array<Comment>,
    canClose: boolean,
    arrowOffset: number
}

export default class LineComments extends React.Component<LineCommentsProps, {}> {
    render() {
        const commentComps = this.props.comments.map((comment) => {
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