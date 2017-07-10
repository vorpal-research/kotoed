import {Comment, LineComments} from "./model";
import * as React from "react";
import moment = require("moment");
import * as cm from "codemirror"
import {render} from "react-dom";

export class CommentComponent extends React.Component<Comment, {}> {
    render() {
        return (
            <div className="panel panel-danger comment">
                <div className="panel-heading comment-heading">
                    {`${this.props.author}  @ ${moment(this.props.dateTime).format('LLLL')}`}
                </div>
                <div className="panel-body">
                    {`${this.props.text}`}
                </div>
            </div>
        );
    }
}

interface LineCommentsProps extends LineComments {
    canClose: boolean,
    arrowOffset: number
}

export class LineCommentsComponent extends React.Component<LineCommentsProps, {}> {
    render() {
        const commentComps = this.props.comments.map((comment) => {
           return (<CommentComponent key={comment.id} {...comment}/>);
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

interface MarkerProps {
    lineComments: LineComments,
    lineNumber: number,
    editor: cm.Editor,
    arrowOffset: number
}

export class MarkerComponent extends React.Component<MarkerProps, { hidden: boolean }> {
    constructor() {
        super();
        this.state = {
            hidden: true
        };
    }

    getClassNames = () => {
        if (this.state.hidden)
            return "label-danger review-hidden";
        else
            return "label-default review-shown"
    };

    onClick = () => {
        if (this.state.hidden) {
            let div = document.createElement("div");
            render(
                <LineCommentsComponent
                    comments={this.props.lineComments.comments}
                    canClose={true}
                    arrowOffset={this.props.arrowOffset}/>,
                div);
            this.props.editor.addLineWidget(this.props.lineNumber, div, {
                coverGutter: true,
                noHScroll: false,
                above: false,
                showIfHidden: false
            });
        } else {
            let li = this.props.editor.lineInfo(this.props.lineNumber);
            li.widgets[0].clear();
        }

        this.setState((prevState) => ({
            hidden: !prevState.hidden
        }));
    };

    render() {
        return (
            <div className="comments-counter-wrapper">
                <span className={`comments-counter label ${this.getClassNames()}`} onClick={this.onClick}>
                    {this.props.lineComments.comments.length}
                </span>
            </div>
        );
    }
}