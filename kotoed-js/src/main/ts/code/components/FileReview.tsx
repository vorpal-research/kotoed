import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import LineMarker from "./LineMarker";
import {fromCmLine, toCmLine} from "../util/codemirror";
import {Comment, FileComments, LineCommentsState} from "../state";
import {List} from "immutable";

export interface FileReviewProps {
    value: string,
    mode?: string,
    contentType?: string,
    height: number | string,
    comments: FileComments,
    onSubmit: (line: number, comment: string) => void
}

interface FileReviewComponentState {
    expanded: Array<boolean>
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReview extends React.Component<FileReviewProps, FileReviewComponentState> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;

    constructor(props: FileReviewProps) {
        super(props);
        this.state = {
            expanded: []
        }
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
        this.setState((prevState, props) => {
            let newExpanded = [...prevState.expanded];
            newExpanded[lineNo] = expanded;
            return {
                expanded: newExpanded
            }
        });
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
            let comments: LineCommentsState = this.props.comments.get(reviewLine, List<Comment>());

            this.cleanUpLine(cmLine);

            let badge = document.createElement("div");
            render(<LineMarker
                    comments={comments}
                    lineNumber={reviewLine}
                    editor={this.editor}
                    arrowOffset={this.arrowOffset}
                    expanded={this.state.expanded[reviewLine]}
                    onExpand={this.handleMarkerExpand}
                    onCollapse={this.handleMarkerCollapse}
                    onSubmit={this.props.onSubmit}
                />,
                badge);
            this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);

        }
    };

    componentDidMount() {
        if (this.props.mode)
            require(`codemirror/mode/${this.props.mode}/${this.props.mode}`);
        this.editor = cm.fromTextArea(this.textAreaNode, {
            lineNumbers: true,
            mode: this.props.contentType || this.props.mode || "text/plain",
            readOnly: true,
            foldGutter: true,
            gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter", "review-gutter"],
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



        this.setState(() => {
            let newExpanded: Array<boolean> = Array<boolean>(this.editor.getDoc().lineCount()).fill(false);
            return {
                expanded: newExpanded
            }
        });

        this.renderMarkers();
    }

    componentDidUpdate(oldProps: FileReviewProps) {
        if (this.props.mode && oldProps.mode !== this.props.mode) {
            require(`codemirror/mode/${this.props.mode}/${this.props.mode}`);
        }

        if (oldProps.mode !== this.props.mode || oldProps.contentType !== this.props.contentType) {
            this.editor.setOption("mode", this.props.contentType || this.props.mode || "text/plain");
        }

        if (oldProps.value !== this.props.value) {
            this.editor.setValue(this.props.value);
        }


        this.renderMarkers();
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
