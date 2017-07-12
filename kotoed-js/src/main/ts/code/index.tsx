import $ = require('jquery');

import "codemirror/mode/clike/clike"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"


import "less/kotoed-bootstrap/bootstrap.less";
import "less/code.less";
import "codemirror/addon/fold/foldgutter.css";
import "codemirror/lib/codemirror.css";
import * as React from "react";

import {render} from "react-dom";
import CodeReviewContainer from "./containers/CodeReviewContainer";
import {BrowserRouter} from "react-router-dom";
import {Route} from "react-router";


// render(
//     <FileReviewComponent
//         mode="clike"
//         contentType="text/x-kotlin"
//         height={800}
//         comments={groupByLine(comments)}
//         value={code}/>,
//     document.getElementById("code-review-app"));

render(
    <BrowserRouter basename="/codereview">
        <div>
            <Route path="/:rev/:path*" component={CodeReviewContainer}/>
        </div>
    </BrowserRouter>,
    document.getElementById("code-review-app"));