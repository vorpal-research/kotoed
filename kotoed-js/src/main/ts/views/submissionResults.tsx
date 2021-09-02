import * as React from "react";
import {render} from "react-dom";
import {ColumnDefinition, components, RowDefinition} from "griddle-react";
import * as _ from "lodash";

import {GenericResponse, IdRequest, sendAsync} from "./components/common";
import {
    ArrayColumn,
    CodeColumn,
    isUnknownFailureInfo,
    KloneColumn,
    TestData,
    TestDataColumn
} from "./components/griddleEx"
import {KloneInfo} from "./components/kloneView";
import {ResultHolder} from "./components/resultHolder";
import {
    ResultListHolder,
    ResultListHolderProps
} from "./components/resultListHolder";

import {fetchPermissions} from "../submissionDetails/remote";
import {Kotoed} from "../util/kotoed-api";
import natsort from "natsort";

export class SubmissionResultTable<ResultT> extends ResultListHolder<any> {
    constructor(props: ResultListHolderProps<ResultT>, context: undefined) {
        super(props, context);
    }

    loadPermissions = () => fetchPermissions(this.props.id);

    loadResults = () => {
        let subResults = sendAsync(Kotoed.Address.Api.Submission.Result.Read, {"id": this.props.id});
        let subReport = sendAsync(Kotoed.Address.Api.Submission.Report, {"id": this.props.id});

        return Promise.all([subResults, subReport]).then(([res, rep]) => {
            const sorter = natsort({insensitive: true}); // Total row should be the last one
            const sortedRep = rep.data.sort((a: string[], b: string[]) =>
                sorter(a[0], b[0])
            );
            const sum: GenericResponse<ResultT> = {
                records: _.concat(res.records, {
                    type: "submission.statistics",
                    data: sortedRep
                } as any),
                verificationData: res.verificationData
            };
            return sum;
        });
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

    export function merger(results: any[]): any[] {
        return results
    }

    export let hideTodosFilter = {
        name: "Hide TODOs",
        predicate: (row: any): boolean => {
            return _.every(row.results, (td: TestData) =>
                td.status == "NOT_IMPLEMENTED" ||
                isUnknownFailureInfo(td.failure)
                && td.failure.nestedException
                && td.failure.nestedException.match(/kotlin\.NotImplementedError/)
            );
        },
        isOnByDefault: true
    };

    export let hideExamplesFilter = {
        name: "Hide examples",
        predicate: (row: any): boolean => {
            return _.some(row.tags, tag =>
                "Example" === tag
            );
        },
        isOnByDefault: true
    };

    export let hidePassedFilter = {
        name: "Hide passed tests",
        predicate: (row: any): boolean => {
            return _.every(row.results, (td: TestData) =>
                "SUCCESSFUL" === td.status
            );
        },
        isOnByDefault: false
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

    export function merger(results: any[]): any[] {
        return results
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
        return result["type"].match(/submission\.statistics/)
    }

    export function transformer(result: any): any[] {
        let res: any[] = [];

        let data: string[][] = result.data;
        let header = data[0];

        for (let i = 1; i < data.length; ++i) {
            let r = {};
            let d = data[i];

            header.map((h, j) => {
                _.set(r, h, d[j]);
            });

            res = res.concat(r)
        }

        return res;
    }

    export function merger(results: any[]): any[] {
        return results
    }

    export let rowDefinition =
        <RowDefinition>
            <ColumnDefinition id=""
                              title="Package"/>
            <ColumnDefinition id="Score"
                              title="Score"/>
            <ColumnDefinition id="Description"
                              title="Description"/>
        </RowDefinition> as any

} // namespace Statistics

namespace Klones {

    export function selector(result: any): boolean {
        return result["type"].match(/^klonecheck$/)
    }

    export function transformer(result: any): any[] {
        return result.body;
    }

    export function merger(results: any[]): any[] {
        let groups = _.groupBy(results, (result) => {
            let baseKlone = result.find(
                (klone: KloneInfo) => submissionId === klone.submissionId
            );

            return baseKlone
                ? `${baseKlone.submissionId}:${baseKlone.file.path}:${baseKlone.fromLine}:${baseKlone.toLine}`
                : "NONE";
        });

        return _.map(groups, (kloneClasses, baseKlone: KloneInfo) => {
            let kloneClass = _.reduce(
                kloneClasses,
                (acc, kloneClass) => _.concat(acc, kloneClass),
                []
            );

            return {
                value: {
                    baseSubmissionId: submissionId,
                    kloneClass: _.uniqBy(
                        kloneClass,
                        (klone: KloneInfo) => `${klone.submissionId}:${klone.file.path}:${klone.fromLine}:${klone.toLine}`
                    )
                }
            };
        });
    }

    export let rowDefinition =
        <RowDefinition>
            <ColumnDefinition id="value"
                              title="Clone class"
                              customComponent={KloneColumn}/>
        </RowDefinition> as any

}

render(
    <SubmissionResultTable
        id={submissionId}
        resultHolders={[
            <ResultHolder name="KFirst"
                          selector={KFirst.selector}
                          transformer={KFirst.transformer}
                          merger={KFirst.merger}
                          filters={[
                              KFirst.hideTodosFilter,
                              KFirst.hideExamplesFilter,
                              KFirst.hidePassedFilter
                          ]}
                          rowDefinition={KFirst.rowDefinition}
                          isVisible={_ => true}/> as any,
            <ResultHolder name="Statistics"
                          selector={Statistics.selector}
                          transformer={Statistics.transformer}
                          merger={Statistics.merger}
                          filters={[]}
                          rowDefinition={Statistics.rowDefinition}
                          isVisible={_ => true}/> as any,
            <ResultHolder name="Build logs"
                          selector={BuildLogs.selector}
                          transformer={BuildLogs.transformer}
                          merger={BuildLogs.merger}
                          filters={[]}
                          rowDefinition={BuildLogs.rowDefinition}
                          isVisible={_ => true}/> as any,
            <ResultHolder name="Klones"
                          selector={Klones.selector}
                          transformer={Klones.transformer}
                          merger={Klones.merger}
                          filters={[]}
                          rowDefinition={Klones.rowDefinition}
                          isVisible={state => state.permissions ? state.permissions.klones : false}/> as any
        ]}/>,
    rootElement
);
