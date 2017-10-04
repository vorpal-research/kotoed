import * as cm from "codemirror"
import * as React from "react";
import {render, unmountComponentAtNode} from "react-dom";

import "codemirror/addon/display/rulers"
import "codemirror/addon/lint/lint.css"
import "codemirror/addon/lint/lint"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"


import LineMarker from "./LineMarker";
import {
    editorModeParam, FOLD_GUTTER, fromCmLine, guessCmModeForFile, LINE_NUMBER_GUTTER, LINT_GUTTER, requireCmMode,
    toCmLine
} from "../util/codemirror";
import {Comment, FileComments, LineComments} from "../state/comments";
import {List} from "immutable";
import {ScrollTo} from "../state/index";
import ComponentWithLoading, {LoadingProperty} from "../../views/components/ComponentWithLoading";
import {BaseCommentToRead} from "../../data/comment";
import {DEFAULT_FORM_STATE, FileForms, FormState, ReviewForms} from "../state/forms";
import {CodeAnnotation} from "../state/annotations";

interface FileReviewBaseProps {
    canPostComment: boolean
    value: string,
    height: number | string,
    comments: FileComments,
    codeAnnotations?: CodeAnnotation[],
    filePath: string,
    whoAmI: string
    scrollTo: ScrollTo
    forms: FileForms
}

interface FileReviewCallbacks {
    onSubmit: (line: number, comment: string) => void
    onCommentUnresolve: (filePath: string, lineNumber: number, id: number) => void
    onCommentResolve: (filePath: string, lineNumber: number, id: number) => void
    onMarkerExpand: (file: string, lineNumber: number) => void
    onMarkerCollapse: (file: string, lineNumber: number) => void
    onHiddenExpand: (file: string, lineNumber: number, comments: List<Comment>) => void
    // TODO the good thing is to pass this callback further
    onCommentEmphasize: (file: string, lineNumber: number, commentId: number) => void
    onCommentEdit: (file: string, line: number, id: number, newText: string) => void
    makeOriginalCommentLink?: (comment: BaseCommentToRead) => string | undefined
}

export type FileReviewProps = FileReviewBaseProps & FileReviewCallbacks & LoadingProperty

interface FileReviewState {
    expanded: Array<boolean>
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReview extends ComponentWithLoading<FileReviewProps, FileReviewState> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;
    private markerDivs: Map<number, HTMLDivElement> = new Map();

    constructor(props: FileReviewProps) {
        super(props);
    }

    private cleanUpLine = (cmLine: number) => {
        let lineInfo = this.editor.lineInfo(cmLine);


        let componentContainer = this.markerDivs.get(cmLine);

        if (componentContainer)
            unmountComponentAtNode(componentContainer);

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

        let badge: HTMLDivElement = document.createElement("div");
        this.markerDivs.set(cmLine, badge);
        this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);
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
                makeOriginalCommentLink={this.props.makeOriginalCommentLink}
                formState={this.props.forms.get(reviewLine) || {processing: false, text: ""}}
            />,
            badge);
    };

    private get lastCommentLocation() {
        return this.props.comments.keySeq().toList().max();
    }

    private get lineCount() {
        return this.props.value.split("\n").length;
    }

    private get fakeLinesCount() {
        let lastLine = toCmLine(this.lastCommentLocation);
        let lineCount = this.lineCount;
        return Math.max(lastLine - lineCount + 1, 0)
    }

    private get processedValue() {
        return this.props.value +
            "\n ".repeat(this.fakeLinesCount)  // Note: space is intended here. CM cannot .markText() an empty line
                                               // (or at least I don't know how to do it)
    };


    private unGrayOutFakeLines() {
        for (let i = this.lineCount; i <= toCmLine(this.lastCommentLocation); i++) {
            this.editor.removeLineClass(i, "text", "cm-fake-line");
            this.editor.removeLineClass(i, "wrap", "cm-fake-line");
            // No need to unmark text here (text is marked for document)
        }
    }

    private grayOutFakeLines() {
        for (let i = this.lineCount; i <= toCmLine(this.lastCommentLocation); i++) {
            this.editor.addLineClass(i, "text", "cm-fake-line");
            this.editor.addLineClass(i, "wrap", "cm-fake-line");
            if (this.props.comments.get(fromCmLine(i), LineComments()).isEmpty()) {
                this.editor.getDoc().markText(
                    {
                        line: i,
                        ch: 0
                    },
                    {
                        line: i,
                        ch: Infinity
                    },
                    {
                        inclusiveRight: true,
                        inclusiveLeft: true,
                        collapsed: true
                    });
            } else {
                this.editor.getDoc().markText(
                    {
                        line: i,
                        ch: 0
                    },
                    {
                        line: i,
                        ch: Infinity
                    },
                    {
                        inclusiveRight: true,
                        inclusiveLeft: true,
                        readOnly: true,
                        atomic: true
                    });
            }
        }
    }


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

    private incrementallyRenderMarkers = (oldProps: FileReviewProps) => {
        let oldFileComments = oldProps.comments;
        let scrollInfo = this.editor.getScrollInfo();
        for (let i = 0; i < this.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            let comments: LineComments = this.props.comments.get(reviewLine, LineComments());
            let oldComments = oldFileComments.get(reviewLine, LineComments());
            let formState: FormState = this.props.forms.get(reviewLine) || DEFAULT_FORM_STATE;
            let oldFormState: FormState = oldProps.forms.get(reviewLine) || DEFAULT_FORM_STATE;
            if (comments !== oldComments || formState !== oldFormState)
                this.renderMarker(cmLine, comments);
        }
        this.editor.scrollTo(scrollInfo.left,  scrollInfo.top)
    };

    private resetExpanded = (props: FileReviewProps) => {
        let newExpanded = Array<boolean>(props.value.split("\n").length).fill(false);

        if (props.scrollTo !== undefined && props.scrollTo.line !== undefined) {
            newExpanded[toCmLine(props.scrollTo.line)] = true;
        }

        this.setState({
            expanded: newExpanded
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

    private scrollToLine = () => {
        if (this.props.scrollTo === undefined)
            return;

        let {line, commentId} = this.props.scrollTo;

        if (line === undefined)
            return;

        let cmLine = toCmLine(line);

        if (cmLine >= this.editor.getDoc().lineCount())
            return;

        if (cmLine + 1 < this.editor.getDoc().lineCount()) {
            this.editor.scrollIntoView({
                from: {
                    line: cmLine,
                    ch: 0
                },
                to: {
                    line: cmLine + 1,
                    ch: 0
                }
            }, 0);
        } else {
            this.editor.scrollTo(null, this.editor.getScrollInfo().height)
        }

        // TODO replace collapsing with scrolling
        if (commentId === undefined)
            return;

        this.props.onCommentEmphasize(
            this.props.filePath,
            line,
            commentId
            )


    };

    getAnnotations = (value: string, options: any, editor: cm.Editor) => {
        let annotations = this.props.codeAnnotations || [];
        return annotations.map(annotation => {
                let message = annotation.message;
                let severity = annotation.severity;
                let {line, col} = annotation.position;
                let {start, end} = editor.getTokenAt(cm.Pos(line - 1, col));
                return {
                    message: message,
                    severity: severity,
                    from: cm.Pos(line - 1, start),
                    to: cm.Pos(line - 1, end)
                }
            }
        );
    };

    componentWillMount() {
        this.resetExpanded(this.props)
    }

    componentDidMount() {
        let newMode = guessCmModeForFile(this.props.filePath);
        requireCmMode(newMode);

        this.editor = cm.fromTextArea(this.textAreaNode, {
            lineNumbers: true,
            mode: editorModeParam(newMode),
            readOnly: true,
            foldGutter: true,
            gutters: [LINE_NUMBER_GUTTER, FOLD_GUTTER, REVIEW_GUTTER, LINT_GUTTER],
            lint: {
                async: false,
                hasGutters: true,
                getAnnotations: this.getAnnotations,
            },
            rulers: [{
                column: 80,
                color: "#f80",
                lineStyle: "dashed",
            },
            {
                column: 100,
                color: "#f00",
                lineStyle: "dashed",
            },
            {
                column: 120,
                color: "#f00",
                lineStyle: "solid",
                width: 3
            }]
        });

        this.editor.setSize("100%", this.props.height);

        this.editor.setValue(this.processedValue);
        this.grayOutFakeLines();

        this.updateArrowOffset();

        this.renderMarkers();
        this.scrollToLine();
    }

    private shouldResetExpanded(props: FileReviewProps, nextProps: FileReviewProps) {
        if (props.filePath !== nextProps.filePath)
            return true;

        if (nextProps.scrollTo.line !== undefined && nextProps.scrollTo.line !== props.scrollTo.line)
            return true;

        return false;
    }

    componentWillReceiveProps(props: FileReviewProps) {
        if (this.shouldResetExpanded(this.props, props)) {
            this.resetExpanded(props)
        }

        if (this.props.filePath !== props.filePath) {
            this.unGrayOutFakeLines()
        }
    }

    componentDidUpdate(oldProps: FileReviewProps) {
        if (oldProps.value !== this.props.value) {
            this.editor.setValue(this.processedValue);
            this.updateArrowOffset();
        }

        if (this.props.filePath !== oldProps.filePath) {
            let newMode = guessCmModeForFile(this.props.filePath);
            requireCmMode(newMode);
            this.editor.setOption("mode", editorModeParam(newMode));
            this.grayOutFakeLines();
        }

        if (this.shouldResetExpanded(oldProps, this.props)) {
            this.renderMarkers();
        }


        if (this.props.filePath === oldProps.filePath &&
                (this.props.comments !== oldProps.comments || this.props.forms !== oldProps.forms)) {
            this.incrementallyRenderMarkers(oldProps);
        }
        if (oldProps.scrollTo.line !== this.props.scrollTo.line || oldProps.scrollTo.commentId !== this.props.scrollTo.commentId)
            this.scrollToLine();
    }

    componentWillUnmount () {
        this.markerDivs.forEach((v: HTMLDivElement) => {
            unmountComponentAtNode(v);
        });

        if (this.editor) {
            this.editor.toTextArea();
        }
    }

    render() {
        return (
            <div className="codemirror-with-veil">
                <div className="codemirror-abs">
                    {this.renderVeil()}
                    <textarea ref={ref => this.textAreaNode = ref as HTMLTextAreaElement} defaultValue={this.props.value}/>
                </div>
            </div>
        )
    }
}
