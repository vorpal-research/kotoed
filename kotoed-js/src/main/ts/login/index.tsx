import * as React from "react"
import {render} from "react-dom";

import "less/kotoed-bootstrap/bootstrap.less"
import "less/login.less"
import {reducer} from "./reducers";
import {applyMiddleware, createStore} from "redux";
import thunk from "redux-thunk";

import SignInOrUpContainer from "./containers/SignInOrUpContainer"
import {Provider} from "react-redux";

export const store = createStore(
    reducer,
    applyMiddleware(thunk)
);

render(
    <div className="login-wrapper">
        <Provider store={store}>
            <SignInOrUpContainer/>
        </Provider>
    </div>,
    document.getElementById("login-app"));