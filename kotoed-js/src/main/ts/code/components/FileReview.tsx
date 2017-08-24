import * as cm from "codemirror"
import * as React from "react";
import {render, unmountComponentAtNode} from "react-dom";

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
import {ScrollTo} from "../state/index";
import ComponentWithLoading, {LoadingProperty} from "../../views/components/ComponentWithLoading";
import {BaseCommentToRead} from "../../data/comment";

interface FileReviewBaseProps {
    canPostComment: boolean
    value: string,
    height: number | string,
    comments: FileComments,
    filePath: string,
    whoAmI: string
    scrollTo: ScrollTo
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
    editor: cm.EditorFromTextArea
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReview extends ComponentWithLoading<FileReviewProps, FileReviewState> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private markerDivs: Map<number, HTMLDivElement> = new Map();

    constructor(props: FileReviewProps) {
        super(props);
    }

    private cleanUpLine = (cmLine: number) => {
        let lineInfo = this.state.editor.lineInfo(cmLine);
        if (lineInfo.gutterMarkers && lineInfo.gutterMarkers[REVIEW_GUTTER]) {
            this.state.editor.setGutterMarker(cmLine, REVIEW_GUTTER, null);
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

    private renderMarker = (cmLine: number) => {
        let reviewLine = fromCmLine(cmLine);
        this.cleanUpLine(cmLine);
        let badge = this.markerDivs.get(cmLine);

        if (badge === undefined)
            throw new Error("Rendering marker for wrong line");

        this.state.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);
    };

    private renderMarkers = () => {
        let scrollInfo = this.state.editor.getScrollInfo();
        for (let i = 0; i < this.state.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            this.renderMarker(cmLine);
        }
        this.state.editor.scrollTo(scrollInfo.left,  scrollInfo.top)
    };

    private incrementallyRenderMarkers = (oldFileComments: FileComments) => {
        let scrollInfo = this.state.editor.getScrollInfo();
        for (let i = 0; i < this.state.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            let comments: LineComments = this.props.comments.get(reviewLine, LineComments());
            let oldComments = oldFileComments.get(reviewLine, LineComments());
            if (comments !== oldComments)
                this.renderMarker(cmLine);
        }
        this.state.editor.scrollTo(scrollInfo.left,  scrollInfo.top)
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
        $(this.state.editor.getGutterElement()).children().each((ix, elem) => {
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

        this.state.editor.scrollIntoView({
            from: {
                line: toCmLine(line),
                ch: 0
            },
            to: {
                line: toCmLine(line + 1),
                ch: 0
            }
        }, 0);

        // TODO replace collapsing with scrolling
        if (commentId === undefined)
            return;

        this.props.onCommentEmphasize(
            this.props.filePath,
            line,
            commentId
            )


    };

    componentWillMount() {
        this.resetExpanded(this.props)
    }

    componentDidMount() {
        let newMode = guessCmModeForFile((this.props.filePath));
        requireCmMode(newMode);

        this.setState({
            editor: cm.fromTextArea(this.textAreaNode, {
                lineNumbers: true,
                mode: editorModeParam(newMode),
                readOnly: true,
                foldGutter: true,
                gutters: [LINE_NUMBER_GUTTER, FOLD_GUTTER, REVIEW_GUTTER],
                lineWrapping: true,
            })
        }, () => {
            this.state.editor.setSize(null, this.props.height);

            this.updateArrowOffset();

            this.renderMarkers();
            this.scrollToLine();
        });


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
    }

    componentDidUpdate(oldProps: FileReviewProps) {
        if (oldProps.value !== this.props.value) {
            this.state.editor.setValue(this.props.value);
            this.updateArrowOffset();
        }

        if (this.props.filePath !== oldProps.filePath) {
            let newMode = guessCmModeForFile((this.props.filePath));
            requireCmMode(newMode);
            this.state.editor.setOption("mode", editorModeParam(newMode));
        }

        if (this.shouldResetExpanded(oldProps, this.props)) {
            this.renderMarkers();
        }


        if (this.props.filePath === oldProps.filePath && this.props.comments !== oldProps.comments) {
            this.incrementallyRenderMarkers(oldProps.comments);
        }
        if (oldProps.scrollTo.line !== this.props.scrollTo.line || oldProps.scrollTo.commentId !== this.props.scrollTo.commentId)
            this.scrollToLine();
    }

    componentWillUnmount () {
        this.markerDivs.forEach((v: HTMLDivElement) => {
            unmountComponentAtNode(v);
        });

        if (this.state.editor) {
            this.state.editor.toTextArea();
        }
    }

    renderMarkerStash = (): Array<JSX.Element> | null => {
        let markerEls: Array<JSX.Element> = [];

        if (this.state.editor === undefined) {
            return null;
        }

        for (let i = 0; i < this.state.editor.getDoc().lineCount(); i++) {
            let cmLine = i;
            let reviewLine = fromCmLine(cmLine);
            let comments: LineComments = this.props.comments.get(reviewLine, LineComments());
            markerEls.push(<div key={cmLine} className="marker-container" ref={(ref) => this.markerDivs.set(cmLine, ref!)}>
                <LineMarker
                    canPostComment={this.props.canPostComment}
                    comments={comments}
                    lineNumber={reviewLine}
                    editor={this.state.editor}
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
                />
            </div>);
        }
        return markerEls;
    };

    render() {
        return (
            <div className="codemirror-with-veil">
                {this.renderVeil()}
                <textarea ref={ref => this.textAreaNode = ref as HTMLTextAreaElement} defaultValue={this.props.value}/>
                <div className="stash" id="marker-stash">
                    {this.renderMarkerStash()}
                </div>
            </div>
        )
    }
}
