import * as React from "react";
import * as ReactRouter from "react-router"

import {default as FileReviewComponent, FileReviewProps} from "../components/FileReview";
import {CmMode, groupByLine, guessCmMode} from "../util";
import * as data_stubs from "../data_stubs"
import {codeJava, codeKt, codePlain, codeScala, comments} from "../data_stubs";
import {createStore} from "redux";
import {connect} from "react-redux";
import CodeReview from "../components/CodeReview";

export const store = createStore((state) => {return state});

const mapStateToProps = function(store): Partial<FileReviewProps> {
    return {
        height: 800,
        comments: groupByLine(comments),
        value: codeKt,
        mode: "clike",
        contentType: "text/x-kotlin",
    }
};

const mapDispatchToProps = function (dispatch, ownProps) {
    return {
        onButtonClick: () => {

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

export default connect(mapStateToProps, mapDispatchToProps)(CodeReview);