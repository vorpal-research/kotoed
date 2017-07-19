import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import {Comment} from "../model"
import LineComments from "./LineComments";

interface LineMarkerProps {
    comments: Array<Comment>,
    lineNumber: number,
    editor: cm.Editor,
    arrowOffset: number,
    expanded: boolean,
    onExpand: (number) => void
    onCollapse: (number) => void
}

interface LineMarkerState {
    expanded: boolean
}

export default class LineMarkerComponent extends React.Component<LineMarkerProps, LineMarkerState> {
    constructor(props) {
        super(props);
        this.state = {
            expanded: props.expanded
        };
    }

    componentDidMount() {
        if (this.state.expanded)
            this.doExpand()
    }

    componentDidUpdate(prevProps, prevState) {
        // todo think
    }

    private getClassNames = () => {
        if (this.state.expanded)
            return "label-default review-shown";
        else
            return "label-danger review-hidden";

    };

    private doExpand = () => {
        let div = document.createElement("div");
        render(
            <LineComments
                comments={this.props.comments}
                canClose={true}
                arrowOffset={this.props.arrowOffset}
            />,
            div);
        this.props.editor.addLineWidget(this.props.lineNumber - 1, div, {
            coverGutter: true,
            noHScroll: false,
            above: false,
            showIfHidden: false
        });
    };

    private doCollapse = () => {
        let li = this.props.editor.lineInfo(this.props.lineNumber  - 1);
        li.widgets[0].clear();
    };

    onClick = () => {
        if (this.state.expanded) {
            this.doCollapse();
            this.props.onCollapse(this.props.lineNumber);
        } else {
            this.doExpand();
            this.props.onExpand(this.props.lineNumber);
        }

        this.setState((prevState) => ({
            expanded: !prevState.expanded
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