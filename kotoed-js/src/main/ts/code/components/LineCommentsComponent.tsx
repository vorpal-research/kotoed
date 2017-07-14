import * as React from "react";

import {Comment} from "../model"
import CommentComponent from "./CommentComponent";

interface LineCommentsProps {
    comments: Array<Comment>,
    canClose: boolean,
    arrowOffset: number
    reduxEx: string
}

export default class LineCommentsComponent extends React.Component<LineCommentsProps, {}> {
    render() {
        const commentComps = this.props.comments.map((comment) => {
            return (<CommentComponent key={comment.id} {...comment} reduxEx={this.props.reduxEx}/>);
        });

        return (
            <div>
                <div className="line-comments col-md-12">
                    {commentComps}
                </div>
                <div className="line-comments-arrow" style={{left: this.props.arrowOffset}}/>
            </div>
        )
    }
}