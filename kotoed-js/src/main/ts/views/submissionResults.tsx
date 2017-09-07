import * as React from "react";
import {render} from "react-dom";
import {ColumnDefinition, components, RowDefinition} from "griddle-react";
import * as _ from "lodash";

import {GenericResponse, IdRequest, sendAsync} from "./components/common";
import {
    ArrayColumn,
    CodeColumn,
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
            return _.every(row.results, (td: TestData) =>
                isUnknownFailureInfo(td.failure)
                && td.failure.nestedException.match(/kotlin\.NotImplementedError/)
            );
        },
        isOnByDefault: true
    };

    export let hideExamplesFilter = {
        name: "Hide examples",
        predicate: (row: any): boolean => {
            return _.some(row.tags, tag => {
                return "Example" === tag;
            });
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

namespace BuildLogs {

    export function selector(result: any): boolean {
        return result["type"].match(/^Failed build log/)
    }

    export function transformer(result: any): any[] {
        return [result]
    }

    export let rowDefinition =
        <RowDefinition>
            <ColumnDefinition id="type"
                              title="Type"/>
            <ColumnDefinition id="body.log"
                              title="Log"
                              customComponent={CodeColumn}/>
        </RowDefinition> as any

} // namespace BuildLogs

namespace Statistics {

    export function selector(result: any): boolean {
        return KFirst.selector(result)
    }

    export function transformer(result: any): any[] {
        let data = result.body.data;

        let packageMap = new Map<string, { taskName: string, tag: string, status: boolean }[]>();
        let processedPackageMap = new Map<string, Map<string, { solved: number, total: number }>>();

        data.forEach((datum: any) => {
            let packageName = datum.packageName.substr(
                0, datum.packageName.lastIndexOf("."));
            let taskName = `${packageName}.${datum.methodName}`;

            let tag = datum.tags[0] || "None";

            let status = !!datum.results.every((r: TestData) => "SUCCESSFUL" == r.status);

            packageMap.set(packageName, (packageMap.get(packageName) || []).concat([{
                taskName: taskName,
                tag: tag,
                status: status
            }]));
        });

        packageMap.forEach((packageData, packageName) => {
            let taskMap = new Map<string, { tag: string, status: boolean }[]>();
            let processedTaskMap = new Map<string, { tag: string, status: boolean }>();
            let tagMap = new Map<string, { solved: number, total: number }>();

            packageData.forEach(({taskName, tag, status}) => {
                taskMap.set(taskName, (taskMap.get(taskName) || []).concat([{
                    tag: tag,
                    status: status
                }]));
            });

            taskMap.forEach((taskData, taskName) => {
                processedTaskMap.set(taskName, {
                    tag: taskData[0].tag,
                    status: taskData.every(({_, status}: any) => status)
                });
            });

            processedTaskMap.forEach(({tag, status}, _) => {
                let oldValue = tagMap.get(tag) || {solved: 0, total: 0};
                tagMap.set(tag, {
                    solved: oldValue.solved + (status ? 1 : 0),
                    total: oldValue.total + 1
                });
            });

            processedPackageMap.set(packageName, tagMap);
        });

        return [...processedPackageMap.entries()].map(([packageName, packageData]) => {
            let res = {packageName: packageName};
            packageData.forEach((stats, tag) => {
                _.set(res, tag, `${stats.solved} / ${stats.total}`);
            });
            return res;
        });
    }

    export let rowDefinition =
        <RowDefinition>
            <ColumnDefinition id="packageName"
                              title="Package"/>
            <ColumnDefinition id="Trivial"
                              title="Trivial"/>
            <ColumnDefinition id="Easy"
                              title="Easy"/>
            <ColumnDefinition id="Normal"
                              title="Normal"/>
            <ColumnDefinition id="Hard"
                              title="Hard"/>
            <ColumnDefinition id="Impossible"
                              title="Impossible"/>
        </RowDefinition> as any

} // namespace Statistics

render(
    <SubmissionResultTable
        id={submissionId}
        resultHolders={[
            <ResultHolder name="KFirst"
                          selector={KFirst.selector}
                          transformer={KFirst.transformer}
                          filters={[
                              KFirst.hideTodosFilter,
                              KFirst.hideExamplesFilter
                          ]}
                          rowDefinition={KFirst.rowDefinition}/> as any,
            <ResultHolder name="Statistics"
                          selector={Statistics.selector}
                          transformer={Statistics.transformer}
                          filters={[]}
                          rowDefinition={Statistics.rowDefinition}/> as any,
            <ResultHolder name="Build logs"
                          selector={BuildLogs.selector}
                          transformer={BuildLogs.transformer}
                          filters={[]}
                          rowDefinition={BuildLogs.rowDefinition}/> as any
        ]}/>,
    rootElement
);
