import $ = require('jquery');

import * as React from "react";
import {render} from "react-dom";
import {BrowserRouter} from "react-router-dom";
import {Route, Switch} from "react-router";
import {Provider} from "react-redux";
import {combineReducers, createStore, applyMiddleware} from "redux";
import thunk from 'redux-thunk';
import {routerMiddleware, push, ConnectedRouter, routerReducer} from 'react-router-redux'
import createHistory from 'history/createBrowserHistory'

import "codemirror/mode/clike/clike"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"

import CodeReviewContainer from "./containers/CodeReviewContainer";
import {commentsReducer, editorReducer, fileTreeReducer} from "./reducers";

import '@blueprintjs/core/dist/blueprint.css';
import "less/kotoed-bootstrap/bootstrap.less";
import "less/code.less";
import "codemirror/lib/codemirror.css";
import "codemirror/addon/fold/foldgutter.css";
import "less/unblueprint.less"

export const history = createHistory({
    basename: "/codereview"
});

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer,
        comments: commentsReducer,
        router: routerReducer
    }),
    applyMiddleware(routerMiddleware(history)),
    applyMiddleware(thunk)
);

render(
    <Provider store={store}>
        <ConnectedRouter history={history} >
            <Switch>
                <Route exact path="/:submissionId(\\d+)/:path*" component={CodeReviewContainer}/>
            </Switch>
        </ConnectedRouter>
    </Provider>,
    document.getElementById("code-review-app"));