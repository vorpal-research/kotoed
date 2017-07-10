/**
 * Created by gagarski on 7/6/17.
 */

import * as moment from 'moment'
import * as cm from "codemirror"
import {Comment} from "./model"
import * as util from "./util"
import * as React from "react";
import {render} from "react-dom";
import {MarkerComponent} from "./components";

// TODO reactify
export class FileReview {

    private editor: cm.Editor;

    constructor(textArea: HTMLTextAreaElement,
                editorConf: cm.EditorConfiguration,
                private readOnly: boolean = true,
                private initialComments: Array<Comment> = []
    ) {
        this.editor = cm.fromTextArea(textArea, editorConf);
        this.editor.setSize(null, 800);

        let arrowOffset = 0.0;

        $(this.editor.getGutterElement()).children().each(function(ix, elem) {
            let jqel = $(elem);
            if (!jqel.hasClass("review-gutter")) {
                arrowOffset += jqel.width();
            } else {
                arrowOffset += jqel.width() / 2;
                return false;
            }
        });
        arrowOffset -= 5;  // Arrow border

        let grouped = util.groupByLine(initialComments);


        for (let line in grouped) {
            let comments = grouped[line];

            let normLine = parseInt(line) - 1;  // CM line numeration is zero-based
            let badge = document.createElement("div");
            render(<MarkerComponent
                        lineComments={{comments: comments}}
                        lineNumber={normLine}
                        editor={this.editor}
                        arrowOffset={arrowOffset}
                    />,
                    badge);
            this.editor.setGutterMarker(normLine, "review-gutter", badge);
        }

    }

}
