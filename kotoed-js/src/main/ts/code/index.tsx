import $ = require('jquery');

import * as React from "react";
import {render} from "react-dom";
import {BrowserRouter} from "react-router-dom";
import {Route, Switch} from "react-router";
import {Provider} from "react-redux";

import "codemirror/mode/clike/clike"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"

import CodeReviewContainer, {store} from "./containers/CodeReviewContainer";

import '@blueprintjs/core/dist/blueprint.css';
import "less/kotoed-bootstrap/bootstrap.less";
import "less/code.less";
import "codemirror/lib/codemirror.css";
import "codemirror/addon/fold/foldgutter.css";
import "less/unblueprint.less"

render(
    <Provider store={store}>
        <BrowserRouter basename="/codereview">
            <Switch>
                <Route path="/:submission/:path*" component={CodeReviewContainer}/>
            </Switch>
        </BrowserRouter>
    </Provider>,
    document.getElementById("code-review-app"));