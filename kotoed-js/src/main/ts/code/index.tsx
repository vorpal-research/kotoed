import createHistory from 'history/createBrowserHistory'
import * as React from "react";
import {render} from "react-dom";
import {Route, Switch} from "react-router";
import {Provider} from "react-redux";
import {combineReducers, createStore, applyMiddleware} from "redux";
import thunk from 'redux-thunk';
import {routerMiddleware, ConnectedRouter, routerReducer} from 'react-router-redux'

import CodeReviewContainer from "./containers/CodeReviewContainer";
import {capabilitiesReducer, commentsReducer, editorReducer, fileTreeReducer} from "./reducers";

import "less/kotoed-bootstrap/bootstrap.less";
import '@blueprintjs/core/dist/blueprint.css';
import "less/unblueprint.less"
import "less/code.less";
import "codemirror/lib/codemirror.css";
import "codemirror/addon/fold/foldgutter.css";


export const history = createHistory({
    basename: "/codereview"
});

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer,
        commentsState: commentsReducer,
        capabilitiesState: capabilitiesReducer,
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