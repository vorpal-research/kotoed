import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import LineMarker from "./LineMarker";
import {CmMode, editorModeParam, FOLD_GUTTER, fromCmLine, LINE_NUMBER_GUTTER, requireCmMode} from "../util/codemirror";
import {Comment, FileComments, LineComments} from "../state";
import {List} from "immutable";

export interface FileReviewProps {
    value: string,
    mode: CmMode,
    height: number | string,
    comments: FileComments,
    filePath: string,
    onSubmit: (line: number, comment: string) => void
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReview extends React.Component<FileReviewProps, {}> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;
    private expanded: Array<boolean>;

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
            this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, null as any);  // wrong typing for last argument
        }

    };

    private handleMarkerSwitch(lineNo: number, expanded: boolean) {
        this.expanded[lineNo] = expanded;
    }

    private handleMarkerExpand = (lineNo: number) => {
        this.handleMarkerSwitch(lineNo, true);
    };

    private handleMarkerCollapse = (lineNo: number) => {
        this.handleMarkerSwitch(lineNo, false);
    };

    private renderMarkers = () => {
        for (let i = 0; i < this.editor.getDoc().lineCount(); i++) {
            // TODO try to be more smart here
            let cmLine = i;
            let reviewLine = fromCmLine(i);
            let comments: LineComments = this.props.comments.get(reviewLine, List<Comment>());

            this.cleanUpLine(cmLine);

            let badge = document.createElement("div");
            render(<LineMarker
                    comments={comments}
                    lineNumber={reviewLine}
                    editor={this.editor}
                    arrowOffset={this.arrowOffset}
                    expanded={this.expanded[reviewLine]}
                    onExpand={this.handleMarkerExpand}
                    onCollapse={this.handleMarkerCollapse}
                    onSubmit={this.props.onSubmit}
                />,
                badge);
            this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);

        }
    };

    private resetExpanded = () => {
        this.expanded = Array<boolean>(this.editor.getDoc().lineCount()).fill(false);
    };

    componentDidMount() {
        requireCmMode(this.props.mode);

        this.editor = cm.fromTextArea(this.textAreaNode, {
            lineNumbers: true,
            mode: editorModeParam(this.props.mode),
            readOnly: true,
            foldGutter: true,
            gutters: [LINE_NUMBER_GUTTER, FOLD_GUTTER, REVIEW_GUTTER],
            lineWrapping: true,
        });

        this.editor.setSize(null, this.props.height);

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

        this.resetExpanded();
        this.renderMarkers();
    }

    componentDidUpdate(oldProps: FileReviewProps) {
        if (oldProps.mode.mode !== this.props.mode.mode) {
            requireCmMode(this.props.mode);
        }

        if (oldProps.mode.mode !== this.props.mode.mode || oldProps.mode.contentType !== this.props.mode.contentType) {
            this.editor.setOption("mode", editorModeParam(this.props.mode));
        }

        if (oldProps.value !== this.props.value) {
            this.editor.setValue(this.props.value);
        }

        if (this.props.filePath !== oldProps.filePath) {
            this.resetExpanded()
        }

        if (this.props.filePath !== oldProps.filePath || this.props.comments !== oldProps.comments) {
            this.renderMarkers();
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
