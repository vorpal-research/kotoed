import * as React from "react";
import {render} from "react-dom";
import {ColumnDefinition, components, RowDefinition} from "griddle-react";
import * as _ from "lodash";

import {GenericResponse, IdRequest, sendAsync} from "./components/common";
import {
    ArrayColumn,
    isUnknownFailureInfo,
    TestData,
    TestDataColumn
} from "./components/griddleEx"
import {ResultHolder} from "./components/resultHolder";
import {
    ResultListHolder,
    ResultListHolderProps
} from "./components/resultListHolder";

import {Kotoed} from "../util/kotoed-api";

export class SubmissionResultTable<ResultT> extends ResultListHolder<any> {
    constructor(props: ResultListHolderProps<ResultT>, context: undefined) {
        super(props, context);
    }

    loadResults = () => {
        return sendAsync<IdRequest, GenericResponse<ResultT>>
        (Kotoed.Address.Api.Submission.Result.Read, {"id": this.props.id})
    };
}

let rootElement = document.getElementById("view-submission-results")!;
let submissionId = Number(rootElement.getAttribute("data-submission-id"));

namespace JUnit {

    export function selector(result: any): boolean {
        return result["type"].match(/TEST-*\.xml/)
    }

    export function transformer(result: any): any[] {
        return [result]
    }

    export let rowDefinition =
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

} // namespace junit

namespace KFirst {

    export function selector(result: any): boolean {
        return result["type"].match(/results\.json/)
    }

    export function transformer(result: any): any[] {
        return result.body.data
    }

    export let hideTodosFilter = {
        name: "Hide TODOs",
        predicate: (row: any): boolean => {
            return _.every(row.results, ((td: TestData) => isUnknownFailureInfo(td.failure)));
        },
        isOnByDefault: true
    };

    export let rowDefinition: components.RowDefinition =
        <RowDefinition>
            <ColumnDefinition id="packageName"
                              title="Package"/>
            <ColumnDefinition id="methodName"
                              title="Method"/>
            <ColumnDefinition id="tags"
                              title="Tags"
                              customComponent={ArrayColumn}/>
            <ColumnDefinition id="results"
                              title="Results"
                              customComponent={TestDataColumn}/>
        </RowDefinition> as any

} // namespace KFirst

render(
    <SubmissionResultTable
        id={submissionId}
        resultHolders={[
            <ResultHolder name="KFirst"
                          selector={KFirst.selector}
                          transformer={KFirst.transformer}
                          filters={[KFirst.hideTodosFilter]}
                          rowDefinition={KFirst.rowDefinition}/> as any
        ]}/>,
    rootElement
);
