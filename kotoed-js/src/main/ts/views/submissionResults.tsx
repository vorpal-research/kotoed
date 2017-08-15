import * as React from "react";
import {render} from "react-dom";
import {ColumnDefinition, RowDefinition} from "griddle-react";

import {GenericResponse, IdRequest, sendAsync} from "./components/common";
import {ResultHolder} from "./components/resultHolder";
import {
    ResultListHolder,
    ResultListHolderProps
} from "./components/resultListHolder";

import {Kotoed} from "../util/kotoed-api";

export class SubmissionResultTable<ResultT> extends ResultListHolder<any> {
    constructor(props: ResultListHolderProps, context: undefined) {
        super(props, context);
    }

    componentWillMount() {
        sendAsync<IdRequest, GenericResponse<ResultT>>
        (Kotoed.Address.Api.Submission.Result.Read, {"id": this.props.id})
            .then(this.processResults);
    }
}

let rootElement = document.getElementById("view-submission-results")!;
let submissionId = Number(rootElement.getAttribute("data-submission-id"));

function junitSelector(result: any): boolean {
    return true
}

render(
    <SubmissionResultTable id={submissionId}>
        <ResultHolder name="JUnit" selector={junitSelector}>
            <RowDefinition>
                <ColumnDefinition id="id"
                                  title="ID"/>
                <ColumnDefinition id="body.testsuite.name"
                                  title="Type"/>
                <ColumnDefinition id="body.testsuite.tests"
                                  title="Total"/>
                <ColumnDefinition id="body.testsuite.errors"
                                  title="Errors"/>
                <ColumnDefinition id="body.testsuite.skipped"
                                  title="Skipped"/>
                <ColumnDefinition id="body.testsuite.failures"
                                  title="Failures"/>
            </RowDefinition>
        </ResultHolder>
    </SubmissionResultTable>,
    document.getElementById("view-submission-results")
);
