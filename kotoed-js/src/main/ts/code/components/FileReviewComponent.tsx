import * as React from "react";
import * as cm from "codemirror"
import {render} from "react-dom";

import {Comment} from "../model";
import LineMarkerComponent from "./LineMarkerComponent";
import {toCmLine} from "../util";

// TODO unexport
export interface FileReviewProps {
    value: string,
    mode?: string,
    contentType?: string,
    height: number,
    comments: Comment[][]
}

export default class FileReviewComponent extends React.Component<FileReviewProps, {}> {
    private textAreaNode: HTMLTextAreaElement;
    private arrowOffset: number;
    private editor: cm.EditorFromTextArea;

    renderMarkers = () => {
        for (let line in this.props.comments) {
            let comments = this.props.comments[line];
            let intLine = parseInt(line);  // CM line numeration is zero-based
            let badge = document.createElement("div");

            render(<LineMarkerComponent
                    comments={comments}
                    lineNumber={intLine}
                    editor={this.editor}
                    arrowOffset={this.arrowOffset}
                />,
                badge);
            this.editor.setGutterMarker(toCmLine(intLine), "review-gutter", badge);
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

        this.renderMarkers()
    }

    componentWillUnmount () {
        if (this.editor) {
            this.editor.toTextArea();
        }
    }

    render() {
        return (
            <textarea
                ref={ref => this.textAreaNode = ref} defaultValue={this.props.value}/>

        )
    }
}
