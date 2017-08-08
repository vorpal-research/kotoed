import * as cm from "codemirror"
import * as React from "react";
import {render} from "react-dom";

import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"

import LineMarker from "./LineMarker";
import {
    editorModeParam, FOLD_GUTTER, fromCmLine, guessCmModeForFile, LINE_NUMBER_GUTTER, requireCmMode,
    toCmLine
} from "../util/codemirror";
import {Comment, FileComments, LineComments} from "../state/comments";
import {List} from "immutable";

export interface FileReviewProps {
    canPostComment: boolean
    value: string,
    height: number | string,
    comments: FileComments,
    filePath: string,
    onSubmit: (line: number, comment: string) => void
    onCommentUnresolve: (filePath: string, lineNumber: number, id: number) => void
    onCommentResolve: (filePath: string, lineNumber: number, id: number) => void
    onMarkerExpand: (file: string, lineNumber: number) => void
    onMarkerCollapse: (file: string, lineNumber: number) => void
    onHiddenExpand: (file: string, lineNumber: number, comments: List<Comment>) => void
    onCommentEdit: (file: string, line: number, id: number, newText: string) => void
    whoAmI: string
}

interface FileReviewState {
    expanded: Array<boolean>
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReview extends React.Component<FileReviewProps, FileReviewState> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;

    constructor(props: FileReviewProps) {
        super(props);
    }

    private cleanUpLine = (cmLine: number) => {
        let lineInfo = this.editor.lineInfo(cmLine);

        if (lineInfo.widgets) {
            for (let widget of lineInfo.widgets) {
                widget.clear();
            }
        }

        if (lineInfo.gutterMarkers && lineInfo.gutterMarkers[REVIEW_GUTTER]) {
            this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, null);
        }

    };

    private handleMarkerSwitch(lineNo: number, expanded: boolean) {
        this.setState((prevState) => {
            let newState = {...prevState};
            newState.expanded[toCmLine(lineNo)] = expanded;
            return newState
        })
    }

    private handleMarkerExpand = (lineNo: number) => {
        this.handleMarkerSwitch(lineNo, true);
        this.props.onMarkerExpand(this.props.filePath, lineNo);
    };

    private handleMarkerCollapse = (lineNo: number) => {
        this.handleMarkerSwitch(lineNo, false);
        this.props.onMarkerCollapse(this.props.filePath, lineNo);
    };

    private renderMarker = (cmLine: number, comments: LineComments) => {
        let reviewLine = fromCmLine(cmLine);
        this.cleanUpLine(cmLine);

        let badge = document.createElement("div");
        render(<LineMarker
                canPostComment={this.props.canPostComment}
                comments={comments}
                lineNumber={reviewLine}
                editor={this.editor}
                arrowOffset={this.arrowOffset}
                expanded={this.state.expanded[cmLine]}
                onExpand={this.handleMarkerExpand}
                onCollapse={this.handleMarkerCollapse}
                onSubmit={this.props.onSubmit}
                onCommentResolve={(lineNumber, id) => this.props.onCommentResolve(this.props.filePath, lineNumber, id)}
                onCommentUnresolve={(lineNumber, id) => this.props.onCommentUnresolve(this.props.filePath, lineNumber, id)}
                onHiddenExpand={(line, comments) => this.props.onHiddenExpand(this.props.filePath, line, comments)}
                onCommentEdit={(line, id, newText) => this.props.onCommentEdit(this.props.filePath, line, id, newText)}
                whoAmI={this.props.whoAmI}
            />,
            badge);
        this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);
    };

    private renderMarkers = () => {
        let scrollInfo = this.editor.getScrollInfo();
        for (let i = 0; i < this.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            let comments: LineComments = this.props.comments.get(reviewLine, LineComments());

            this.renderMarker(cmLine, comments);
        }
        this.editor.scrollTo(scrollInfo.left,  scrollInfo.top)
    };

    private incrementallyRenderMarkers = (oldFileComments: FileComments) => {
        let scrollInfo = this.editor.getScrollInfo();
        for (let i = 0; i < this.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            let comments: LineComments = this.props.comments.get(reviewLine, LineComments());
            let oldComments = oldFileComments.get(reviewLine, LineComments());
            if (comments !== oldComments)
                this.renderMarker(cmLine, comments);
        }
        this.editor.scrollTo(scrollInfo.left,  scrollInfo.top)
    };

    private resetExpanded = (value: string) => {
        this.setState({
            expanded: Array<boolean>(value.split("\n").length).fill(false)
        });
    };

    private updateArrowOffset = () => {
        this.arrowOffset = 0.0;
        $(this.editor.getGutterElement()).children().each((ix, elem) => {
            let jqel = $(elem);
            let width = jqel.width();
            if (width !== undefined) {
                if (!jqel.hasClass("review-gutter")) {
                    this.arrowOffset += width;
                } else {
                    this.arrowOffset += width / 2;
                    return false;
                }
            }
        });
        this.arrowOffset -= 5;  // TODO find a way to remove hardcoded 5
    };

    componentWillMount() {
        this.resetExpanded(this.props.value)
    }

    componentDidMount() {
        let newMode = guessCmModeForFile((this.props.filePath));
        requireCmMode(newMode);

        this.editor = cm.fromTextArea(this.textAreaNode, {
            lineNumbers: true,
            mode: editorModeParam(newMode),
            readOnly: true,
            foldGutter: true,
            gutters: [LINE_NUMBER_GUTTER, FOLD_GUTTER, REVIEW_GUTTER],
            lineWrapping: true,
        });

        this.editor.setSize(null, this.props.height);

        this.updateArrowOffset();

        this.renderMarkers();
    }

    componentWillReceiveProps(props: FileReviewProps) {
        if (this.props.filePath !== props.filePath) {
            this.resetExpanded(props.value)
        }
    }

    componentDidUpdate(oldProps: FileReviewProps) {
        if (oldProps.value !== this.props.value) {
            this.editor.setValue(this.props.value);
            this.updateArrowOffset();
        }

        if (this.props.filePath !== oldProps.filePath) {
            let newMode = guessCmModeForFile((this.props.filePath));
            requireCmMode(newMode);
            this.editor.setOption("mode", editorModeParam(newMode));
            this.renderMarkers();
        }

        if (this.props.filePath === oldProps.filePath && this.props.comments !== oldProps.comments) {
            this.incrementallyRenderMarkers(oldProps.comments);
        }
    }

    componentWillUnmount () {
        if (this.editor) {
            this.editor.toTextArea();
        }
    }

    render() {
        return (
            <textarea ref={ref => this.textAreaNode = ref as HTMLTextAreaElement} defaultValue={this.props.value}/>
        )
    }
}
