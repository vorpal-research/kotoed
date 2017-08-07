import * as cm from "codemirror"
import * as React from "react";
import {render} from "react-dom";

import LineComments from "./LineComments";
import {List} from "immutable";
import {Comment} from "../state/comments";

interface LineMarkerProps {
    canPostComment: boolean
    comments: List<Comment>,
    lineNumber: number,
    editor: cm.Editor,
    arrowOffset: number,
    expanded: boolean,
    onExpand: (number: number) => void
    onCollapse: (number: number) => void
    onSubmit: (line: number, comment: string) => void
    onCommentUnresolve: (lineNumber: number, id: number) => void
    onCommentResolve: (lineNumber: number, id: number) => void
    onHiddenExpand: (lineNumber: number, comments: List<Comment>) => void
}

interface LineMarkerState {
    expanded: boolean
}

export default class LineMarkerComponent extends React.Component<LineMarkerProps, LineMarkerState> {
    constructor(props: LineMarkerProps) {
        super(props);
        this.state = {
            expanded: props.expanded
        };
    }

    componentDidMount() {
        if (this.state.expanded)
            this.doExpand()
    }

    componentDidUpdate(prevProps: LineMarkerProps, prevState: LineMarkerState) {
        // todo think
    }

    private getLabelClasses = () => {
        let expandedClass =  (this.state.expanded) ? "review-shown" : "review-hidden";
        let colorClass = (this.state.expanded || this.props.comments.every((c: Comment) => c.state === "closed")) ?
            "label-default" :
            "label-danger";
        return `${expandedClass} ${colorClass}`
    };

    private doExpand = () => {
        let div = document.createElement("div");
        render(
            <LineComments
                canPostComment={this.props.canPostComment}
                comments={this.props.comments}
                arrowOffset={this.props.arrowOffset}
                onSubmit={(text) => this.props.onSubmit(this.props.lineNumber, text)}
                onCommentResolve={id => this.props.onCommentResolve(this.props.lineNumber, id)}
                onCommentUnresolve={id => this.props.onCommentUnresolve(this.props.lineNumber, id)}
                onExpand={(comments) => this.props.onHiddenExpand(this.props.lineNumber, comments)}
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

    getPencilWrapperClasses = () => {
        if (this.state.expanded)
            return "comment-form-shown";
        else
            return "comment-form-hidden";
    };

    renderCounter() {
        return (
            <div className="comments-counter-wrapper">
                <span className={`comments-counter label ${this.getLabelClasses()}`} onClick={this.onClick}>
                    {this.props.comments.size}
                </span>
            </div>
        );
    }

    renderPencil() {
        return (
            <div className={`comments-pencil-wrapper ${this.getPencilWrapperClasses()}`}>
                <span className={`comments-pencil glyphicon glyphicon-pencil`} onClick={this.onClick}/>
            </div>
        );
    }

    render() {
        if (this.props.comments.size == 0)
            return this.renderPencil();
        else
            return this.renderCounter();
    }
}