import * as React from "react"
import {render} from "react-dom";
import createHistory from 'history/createBrowserHistory'

import "less/kotoed-bootstrap/bootstrap.less"
import "less/login.less"
import {reducer} from "./reducers";
import {applyMiddleware, createStore} from "redux";
import thunk from "redux-thunk";

import SignInOrUpContainer from "./containers/SignInOrUpContainer"
import {Provider} from "react-redux";
import {ConnectedRouter} from "react-router-redux";
import {Route, Switch} from "react-router";

export const store = createStore(
    reducer,
    applyMiddleware(thunk)
);

export const history = createHistory({
    basename: "/login"
});


render(
    <div className="login-wrapper">
        <Provider store={store}>
            <ConnectedRouter history={history} >
                <Switch>
                    <Route exact path="/*" component={SignInOrUpContainer}/>
                </Switch>
            </ConnectedRouter>
        </Provider>
    </div>,
    document.getElementById("login-app"));
