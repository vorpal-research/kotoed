import $ = require('jquery');

import * as cm from "codemirror"
import "codemirror/mode/clike/clike"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"
import * as moment from "moment"
import * as cmr from "./cm_review";

import "less/kotoed-bootstrap/bootstrap.less";
import "less/code.less";
import "codemirror/addon/fold/foldgutter.css";
import "codemirror/lib/codemirror.css";

$(document).ready(function() {
    let fr = new cmr.FileReview(
        document.getElementById("editor") as HTMLTextAreaElement,
        {
            lineNumbers: true,
            mode: "text/x-kotlin",
            readOnly: "nocursor",
            foldGutter: true,
            gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter", "review-gutter"],
            lineWrapping: true,
        },
        null,
        true,
        [
            new cmr.VintageComment(
                "Привет!",
                moment("1941-06-22"),
                "Joseph V. Stalin",
                new cmr.CommentLocation("somefile", 2)),
            new cmr.VintageComment(
                "Guten tag!",
                moment("1941-06-23"),
                "Adolf Hitler",
                new cmr.CommentLocation("somefile", 2)),
            new cmr.VintageComment(
                "Hello!",
                moment("1941-06-24"),
                "Franklin D. Roosevelt",
                new cmr.CommentLocation("somefile", 2)),
            new cmr.BootstrapComment(
                "FAKE CODE!",
                moment("2017-01-20"),
                "Donald J. Trump",
                new cmr.CommentLocation("somefile", 8)),
            new cmr.BootstrapComment(
                "Кхе-кхе",
                moment("2017-01-20"),
                "Vladimir V. Putin",
                new cmr.CommentLocation("somefile", 8)),

        ]
    )


});