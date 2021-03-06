import createHistory from 'history/createBrowserHistory'
import * as React from "react";
import {render} from "react-dom";
import {Redirect, Route, Switch} from "react-router";
import {Provider} from "react-redux";
import {combineReducers, createStore, applyMiddleware} from "redux";
import thunk from 'redux-thunk';
import {routerMiddleware, ConnectedRouter, routerReducer} from 'react-router-redux'

import CodeReviewContainer, {
    CODE_ROUTE_PATH, LOST_FOUND_ROUTE_PATH,
    RedirectToRoot
} from "./containers/CodeReviewContainer";
import {
    annotationsReducer,
    capabilitiesReducer, commentsReducer, commentTemplateReducer, editorReducer, fileTreeReducer, formReducer,
    submissionReducer
} from "./reducers";

import "less/kotoed-bootstrap/bootstrap.less";
import "less/bootstrap-xl.less";
import '@blueprintjs/core/dist/blueprint.css';
import "less/unblueprint.less"
import "less/code.less";
import "codemirror/lib/codemirror.css";
import "codemirror/addon/fold/foldgutter.css";

export const history = createHistory();

export const store = createStore(
    combineReducers({
        fileTreeState: fileTreeReducer,
        editorState: editorReducer,
        commentsState: commentsReducer,
        codeAnnotationsState: annotationsReducer,
        commentTemplateState: commentTemplateReducer,
        capabilitiesState: capabilitiesReducer,
        submissionState: submissionReducer,
        formState: formReducer,
        router: routerReducer
    }),
    applyMiddleware(routerMiddleware(history)),
    applyMiddleware(thunk)
);

render(
    <Provider store={store}>
        <ConnectedRouter history={history} >
            <Switch>
                <Route exact path="/submission/:submissionId(\\d+)/review" component={RedirectToRoot}/>
                <Route exact path={CODE_ROUTE_PATH} component={CodeReviewContainer}/>
                <Route exact path={LOST_FOUND_ROUTE_PATH} component={CodeReviewContainer}/>
            </Switch>
        </ConnectedRouter>
    </Provider>,
    document.getElementById("code-review-app"));