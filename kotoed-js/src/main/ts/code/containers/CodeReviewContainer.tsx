import * as React from "react";
import * as ReactRouter from "react-router"
import * as cm from "codemirror"
import {render} from "react-dom";

import {default as FileReviewComponent, FileReviewProps} from "../components/FileReviewComponent";
import {CmMode, groupByLine, guessCmMode} from "../util";
import * as data_stubs from "../data_stubs"
import {codeJava, codeKt, codePlain, codeScala, comments} from "../data_stubs";
import {createStore} from "redux";
import {exampleReducer} from "../reducers";
import {setExampleState} from "../actions";
import {connect} from "react-redux";

export const store = createStore(exampleReducer);

const mapStateToProps = function(store): Partial<FileReviewProps> {
    return {
        height: 800,
        comments: groupByLine(comments),
        value: codeKt,
        mode: "clike",
        contentType: "text/x-kotlin",
        reduxEx: store.reduxEx,
    }
};

const states = ["1", "2", "3", "4"];

const mapDispatchToProps = function (dispatch, ownProps) {
    return {
        onButtonClick() {
            let ix = states.indexOf(this.reduxEx);
            console.log(ix);
            console.log(states.indexOf(this.reduxEx));
            console.log(this.reduxEx);
            console.log(this);

            dispatch(setExampleState(states[((ix + 1) % states.length + states.length) % states.length]))
        }
    }
};

function chooseCode(mode: CmMode) {
    switch(mode.contentType) {
        case "text/x-kotlin": return codeKt;
        case "text/x-java": return codeJava;
        case "text/x-scala": return codeScala;
        default: return codePlain;
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(FileReviewComponent);