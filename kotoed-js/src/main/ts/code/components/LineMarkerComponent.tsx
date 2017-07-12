import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import {Comment} from "../model"
import LineCommentsComponent from "./LineCommentsComponent";

interface LineMarkerProps {
    comments: Array<Comment>,
    lineNumber: number,
    editor: cm.Editor,
    arrowOffset: number
}

interface LineMarkerState {
    hidden: boolean
}

export default class LineMarkerComponent extends React.Component<LineMarkerProps, LineMarkerState> {
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
                    comments={this.props.comments}
                    canClose={true}
                    arrowOffset={this.props.arrowOffset}/>,
                div);
            this.props.editor.addLineWidget(this.props.lineNumber - 1, div, {
                coverGutter: true,
                noHScroll: false,
                above: false,
                showIfHidden: false
            });
        } else {
            let li = this.props.editor.lineInfo(this.props.lineNumber  - 1);
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
                    {this.props.comments.length}
                </span>
            </div>
        );
    }
}