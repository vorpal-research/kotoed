import * as cm from "codemirror"
import * as React from "react";
import {render, unmountComponentAtNode} from "react-dom";
import {Label, Glyphicon} from "react-bootstrap";

import LineComments from "./LineComments";
import {List} from "immutable";
import {Comment} from "../state/comments";
import {LineWidget} from "codemirror";
import {BaseCommentToRead} from "../../data/comment";
import {CommentAggregate} from "../remote/comments";
import AggregatesLabel from "../../views/AggregatesLabel";

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
    onCommentEdit: (line: number, id: number, newText: string) => void
    makeOriginalCommentLink?: (comment: BaseCommentToRead) => string | undefined
    whoAmI: string
}

interface LineMarkerState {
    expanded: boolean
}

export default class LineMarkerComponent extends React.Component<LineMarkerProps, LineMarkerState> {
    private widget: LineWidget | undefined;
    private container: HTMLDivElement = document.createElement("div");
    constructor(props: LineMarkerProps) {
        super(props);
        this.widget = undefined;
        this.state = {
            expanded: props.expanded && (props.comments.size !== 0 || props.canPostComment)
        };
    }

    componentDidMount() {
        if (this.state.expanded)
            this.doExpand()
    }

    componentDidUpdate(prevProps: LineMarkerProps, prevState: LineMarkerState) {
        // todo think
    }

    private getLabelExpanedClass = () => {
        return this.state.expanded ? "review-shown" : "review-hidden";
    };

    
    handleLineWidgetChanged = () => {
        if (this.widget)
            this.widget.changed();
        else
            throw new Error("No widget to change (should not happen)")
    };

    private doExpand = () => {
        this.widget = this.props.editor.addLineWidget(this.props.lineNumber - 1, this.container, {
            coverGutter: true,
            noHScroll: false,
            above: false,
            showIfHidden: false
        });
        render(
            <LineComments
                canPostComment={this.props.canPostComment}
                comments={this.props.comments}
                arrowOffset={this.props.arrowOffset}
                onSubmit={(text) => this.props.onSubmit(this.props.lineNumber, text)}
                onCommentResolve={id => this.props.onCommentResolve(this.props.lineNumber, id)}
                onCommentUnresolve={id => this.props.onCommentUnresolve(this.props.lineNumber, id)}
                onExpand={(comments) => this.props.onHiddenExpand(this.props.lineNumber, comments)}
                notifyEditorAboutChange={this.handleLineWidgetChanged}
                onEdit={(id, newText) => this.props.onCommentEdit(this.props.lineNumber, id, newText)}
                makeOriginalLink={this.props.makeOriginalCommentLink}
                whoAmI={this.props.whoAmI}
            />,
            this.container);
        this.widget.changed();
    };

    private doCollapse = () => {
        unmountComponentAtNode(this.container);
        let li = this.props.editor.lineInfo(this.props.lineNumber  - 1);
        li.widgets[0].clear();
        this.widget = undefined;
    };

    onClick = () => {
        if (this.props.comments.size == 0 && !this.props.canPostComment)
            return;
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
        let agg: CommentAggregate = {
            open: this.props.comments.filter((c: Comment) => c.state === "open").count(),
            closed: this.props.comments.filter((c: Comment) => c.state === "closed").count()
        };
        return (
            <div className="comments-counter-wrapper" onClick={this.onClick}>
                <AggregatesLabel {...agg}/>
            </div>
        );
    }

    renderPencil() {
        return (
            <div className={`comments-pencil-wrapper ${this.getPencilWrapperClasses()}`}>
                <Glyphicon glyph="pencil" onClick={this.onClick}/>
            </div>
        );
    }

    componentWillUnmount() {
        unmountComponentAtNode(this.container);
    }

    render() {
        if (this.props.comments.size == 0 && this.props.canPostComment)
            return this.renderPencil();
        else if (this.props.comments.size == 0)
            return null;
        else
            return this.renderCounter();
    }
}