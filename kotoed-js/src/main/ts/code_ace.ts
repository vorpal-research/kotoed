import $ = require('jquery');

require("less/code.less");


import * as ace from 'brace';
import 'brace/mode/kotlin';
import 'brace/theme/github';
import 'brace/ext/beautify';

import 'ts/ace_review_ext';
import * as ar from "./ace_review";

$(document).ready(function() {
    const editor = ace.edit("editor");
    editor.setTheme("ace/theme/github");
    editor.getSession().setMode("ace/mode/kotlin");
    editor.setReadOnly(true);
    let fr = new ar.FileReview(
        editor,
        // <ace_review.CommentPublisher>null,
        true,
        [
            new ar.DummyComment("hello1", new ar.CommentLocation("fuck", 2)),
            new ar.DummyComment("hello2", new ar.CommentLocation("fuck", 2)),
            new ar.DummyComment("hello3", new ar.CommentLocation("fuck", 2)),
            new ar.DummyComment("hello4", new ar.CommentLocation("fuck", 2)),
            new ar.DummyComment("hello5", new ar.CommentLocation("fuck", 2)),
            new ar.DummyComment("hello6", new ar.CommentLocation("fuck", 2)),

        ]
    )

});

