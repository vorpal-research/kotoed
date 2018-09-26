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

export class SubmissionResultTable<ResultT> extends ResultListHolder<any> {
    constructor(props: ResultListHolderProps<ResultT>, context: undefined) {
        super(props, context);
    }

    loadPermissions = () => fetchPermissions(this.props.id);

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

    export function merger(results: any[]): any[] {
        return results
    }

    export let hideTodosFilter = {
        name: "Hide TODOs",
        predicate: (row: any): boolean => {
            return _.every(row.results, (td: TestData) =>
                td.status == "NOT_IMPLEMENTED" ||
                isUnknownFailureInfo(td.failure)
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
        return KFirst.selector(result)
    }

    function lesson2score(lesson: number[]): number {
        let lessonScore;

        switch (lesson.length) {
            case 0:
                lessonScore = 0.0;
                break;
            case 1:
                lessonScore = lesson[0];
                break;
            default: {
                let [first, second] = lesson;
                if (first == second) lessonScore = first + 1;
                else lessonScore = first;
                break;
            }
        }

        return lessonScore;
    }

    function getLessonPoints(packageData: Map<string, { solved: number, total: number }>): string {
        let solvedScores = _.concat(
            _.times((packageData.get("Impossible") || {solved: 0}).solved, () => 10),
            _.times((packageData.get("Hard") || {solved: 0}).solved, () => 7),
            _.times((packageData.get("Normal") || {solved: 0}).solved, () => 4),
            _.times((packageData.get("Easy") || {solved: 0}).solved, () => 1),
            _.times((packageData.get("Trivial") || {solved: 0}).solved, () => 0),
        );

        let totalScores = _.concat(
            _.times((packageData.get("Impossible") || {total: 0}).total, () => 10),
            _.times((packageData.get("Hard") || {total: 0}).total, () => 7),
            _.times((packageData.get("Normal") || {total: 0}).total, () => 4),
            _.times((packageData.get("Easy") || {total: 0}).total, () => 1),
            _.times((packageData.get("Trivial") || {total: 0}).total, () => 0),
        );

        let res = lesson2score(solvedScores) / lesson2score(totalScores);

        return (isFinite(res) ? res : 0.0).toFixed(2);
    }

    export function transformer(result: any): any[] {
        let data = result.body.data;

        let packageMap = new Map<string, { taskName: string, tag: string, status: boolean }[]>();
        let processedPackageMap = new Map<string, Map<string, { solved: number, total: number }>>();

        data.forEach((datum: any) => {
            let packageName = datum.packageName.substr(
                0, datum.packageName.indexOf("."));
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
            _.set(res, "Points", getLessonPoints(packageData));
            return res;
        });
    }

    export function merger(results: any[]): any[] {
        return results
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
            <ColumnDefinition id="Points"
                              title="Points"/>
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
