import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import {Comment} from "../model";
import LineMarkerComponent from "./LineMarkerComponent";
import {fromCmLine, toCmLine} from "../util";
import {Reducer} from "redux";
import {store} from "../containers/CodeReviewContainer";
import {setExampleState} from "../actions";

// TODO unexport
export interface FileReviewProps {
    value: string,
    mode?: string,
    contentType?: string,
    height: number,
    comments: Comment[][],
    reduxEx: string,
    onButtonClick: () => void
}

interface FileReviewComponentState {
    expanded: Array<boolean>
}

const REVIEW_GUTTER = "review-gutter";

export default class FileReviewComponent extends React.Component<FileReviewProps, FileReviewComponentState> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;

    constructor(props) {
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
            this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, null);
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
            // TODO try to more smart here
            let cmLine = i;
            let reviewLine = fromCmLine(i);
            let comments: Array<Comment> = this.props.comments[reviewLine] || [];

            this.cleanUpLine(cmLine);

            if (comments.length > 0) {
                let badge = document.createElement("div");
                render(<LineMarkerComponent
                        comments={comments}
                        lineNumber={reviewLine}
                        editor={this.editor}
                        arrowOffset={this.arrowOffset}
                        expanded={this.state.expanded[reviewLine]}
                        reduxEx={this.props.reduxEx}
                        onExpand={this.handleMarkerExpand}
                        onCollapse={this.handleMarkerCollapse}
                    />,
                    badge);
                this.editor.setGutterMarker(cmLine, REVIEW_GUTTER, badge);
            }
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
            if (!jqel.hasClass("review-gutter")) {
                this.arrowOffset += jqel.width();
            } else {
                this.arrowOffset += jqel.width() / 2;
                return false;
            }
        });
        this.arrowOffset -= 5;  // TODo find a way to remove hardcoded 5



        this.setState(() => {
            // TODO Enable ES6 and replace with Array.fill()
            let newExpanded: Array<boolean> = [];
            for (let i = 0; i < this.editor.getDoc().lineCount(); i++) {
                newExpanded.push(false);
            }
            return {
                expanded: newExpanded
            }
        });

        this.renderMarkers();
    }

    componentDidUpdate() {
        this.renderMarkers();
    }

    componentWillUnmount () {
        if (this.editor) {
            this.editor.toTextArea();
        }
    }

    render() {
        return (
        <div>
            <textarea ref={ref => this.textAreaNode = ref} defaultValue={this.props.value}/>
            <div>{this.props.reduxEx}</div>
            <button type="button" className="btn btn-primary btn-md" onClick={() => {
                console.log("Clicked!");
                this.props.onButtonClick();
            }}>Click me!</button>
        </div>
        )
    }
}
