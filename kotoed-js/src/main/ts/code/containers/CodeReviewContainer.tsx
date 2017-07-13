import * as React from "react";
import * as ReactRouter from "react-router"
import * as cm from "codemirror"
import {render} from "react-dom";

import {default as FileReviewComponent, FileReviewProps} from "../components/FileReviewComponent";
import {CmMode, groupByLine, guessCmMode} from "../util";
import * as data_stubs from "../data_stubs"
import {codeJava, codeKt, codePlain, codeScala, comments} from "../data_stubs";


function chooseCode(mode: CmMode) {
    switch(mode.contentType) {
        case "text/x-kotlin": return codeKt;
        case "text/x-java": return codeJava;
        case "text/x-scala": return codeScala;
        default: return codePlain;
    }
}

export default class CodeReviewContainer extends
        React.Component<ReactRouter.RouteComponentProps<{splat: Array<string>}>, FileReviewProps> {
    constructor(props) {
        super(props);
        let mode = guessCmMode(this.props.match.params["path"]);
        this.state = {
            height: 800,
            comments: groupByLine(comments),
            value: chooseCode(mode),
            ...mode
        }
    }

    render() {
        console.log(this.props);
        return <FileReviewComponent
            mode={this.state.mode}
            contentType={this.state.contentType}
            height={this.state.height}
            comments={this.state.comments}
            value={this.state.value}/>
    }
}