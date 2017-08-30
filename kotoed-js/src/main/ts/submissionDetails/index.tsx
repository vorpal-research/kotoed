import * as React from "react";
import {render} from "react-dom";
import {SubmissionToRead} from "../data/submission";
import SubmissionDetails from "./components/SubmissionDetails";
import {applyMiddleware, combineReducers, createStore} from "redux";
import {routerMiddleware} from "react-router-redux";
import thunk from "redux-thunk";
import {Provider} from "react-redux";
import {Kotoed} from "../util/kotoed-api";
import snafuDialog from "../util/snafuDialog";
import SubmissionDetailsContainer from "./containers/SubmissionDetailsContainer";
import {reducer} from "./reducers";

export const store = createStore(
    reducer,
    applyMiddleware(thunk)
);

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Submission.Index, window.location.pathname);
if (params == null) {
    snafuDialog();
    throw new Error("Cannot resolve submission id")
}

let id = params.get("id");

if (id === undefined) {
    snafuDialog();
    throw new Error("Cannot resolve submission id")
}

let id_ = parseInt(id);

if (isNaN(id_)) {
    snafuDialog();
    throw new Error("Cannot resolve submission id")
}

render(
    <Provider store={store}>
        <SubmissionDetailsContainer id={id_}/>
    </Provider>,
    document.getElementById("submission-details-app"));