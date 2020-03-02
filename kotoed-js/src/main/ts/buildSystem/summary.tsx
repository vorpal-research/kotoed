import * as React from "react";
import {render} from "react-dom";
import {Kotoed} from "../util/kotoed-api";
import {sendAsync} from "../views/components/common";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {Table} from "react-bootstrap/lib";
import moment = require("moment");
import {poll, SimplePollingStrategy} from "../util/poll";
import {KotoedLink} from "../views/components/Link";
import {KotoedTable, KotoedTableDescription} from "../views/components/KotoedTable";
import UrlPattern = Kotoed.UrlPattern;
import _ = require("lodash");

type BuildSummaryState = { loading: true }
    | { loading: false, summary: BuildStatus[] }

class BuildSummaryView extends React.Component<{}, BuildSummaryState> {
    constructor(props: {}) {
        super(props);
        this.state = {loading: true};
    }

    tryLoad = async () => {
        const remote = await sendAsync(Kotoed.Address.Api.BuildSystem.Build.Summary, undefined);
        const newState = {loading: false, summary: remote};
        this.setState(newState);
    };

    componentDidMount(): void {
        poll({
            action: this.tryLoad,
            isGoodEnough: () => false,
            strategy: new SimplePollingStrategy({interval: 3000, shouldGiveUp: () => false})
        })
    }

    tableDescription: KotoedTableDescription<BuildStatus> = [
        ["Build id", {
            cellClassName: "col-md-1",
            element: (state: BuildStatus) => <KotoedLink pattern={UrlPattern.BuildSystem.Status}
                                                         id={state.request.buildId}>
                {state.request.buildId}
            </KotoedLink>
        }],
        ["Submission id", {
            cellClassName: "col-md-2",
            element: (state: BuildStatus) => <KotoedLink pattern={UrlPattern.Submission.Index}
                                                         id={state.request.submissionId}>
                {state.request.submissionId}
            </KotoedLink>
        }],
        ["Started at", {
            cellClassName: "col-md-2",
            element: (state: BuildStatus) => moment(state.startTime).format('LTS')
        }],
        ["Current command", {
            cellClassName: "col-md-7",
            element: (state: BuildStatus) => <pre>
                {
                    (_.findLast(state.commands, {state: 'RUNNING'}) ||
                        _.findLast(state.commands, {state: 'WAITING'}) ||
                        {commandLine: "DONE"}).commandLine
                }
            </pre>
        }]
    ];

    render(): JSX.Element {
        if (this.state.loading) {
            return <SpinnerWithVeil/>;
        } else {
            return <KotoedTable
                data={this.state.summary}
                description={this.tableDescription}
            />
        }
    }
}

render(
    <BuildSummaryView/>,
    document.getElementById("build-summary-app")
);