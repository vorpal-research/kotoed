import * as React from "react";
import {render} from "react-dom";
import {Kotoed} from "../util/kotoed-api";
import {sendAsync} from "../views/components/common";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {Table} from "react-bootstrap/lib";
import moment = require("moment");
import {poll, SimplePollingStrategy} from "../util/poll";

type BuildSummaryState = { loading: true }
    | { loading: false, summary: BuildStatus[] }

class BuildSummaryView extends React.Component<{}, BuildSummaryState> {
    constructor(props: {}) {
        super(props);
        this.state = { loading: true };
    }

    tryLoad = async () => {
        const remote = await sendAsync(Kotoed.Address.Api.BuildSystem.Build.Summary, {}) as BuildStatus[];
        const newState = {loading: false, summary: remote};
        this.setState(newState);
    };

    componentDidMount(): void {
        poll({
            action: this.tryLoad,
            isGoodEnough: () => false,
            strategy: new SimplePollingStrategy({ interval: 3000, shouldGiveUp: () => false })
        })
    }

    render(): JSX.Element {
        if (this.state.loading) {
            return <SpinnerWithVeil/>;
        } else {
            return <Table striped bordered condensed hover responsive>
                <thead>
                <tr>
                    <th className="col-md-1">Build id</th>
                    <th className="col-md-2">Submission id</th>
                    <th className="col-md-2">Started at</th>
                    <th className="col-md-7">Current command</th>
                </tr>
                </thead>
                <tbody>
                { this.state.summary.map((v, i) => {
                    const submissionId = v.request.submissionId;
                    const buildId = v.request.buildId;
                    return <tr key={`${i}`}>
                        <td className="col-md-1">
                            <a href={
                                Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.BuildSystem.Status, { id: buildId })
                            }>
                                {buildId}
                            </a>
                        </td>
                        <td className="col-md-2">
                            <a href={
                                    Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, { id: submissionId })
                               }>
                                {submissionId}
                            </a>
                        </td>
                        <td className="col-md-2">
                            {moment(v.startTime).format('LTS')}
                        </td>
                        <td className="col-md-7"><pre>{
                            (v.commands.filter(it => it.state === 'RUNNING').pop()
                                || v.commands.filter(it => it.state === 'WAITING').pop()
                                || {commandLine: ""}).commandLine
                        }</pre></td>
                    </tr>
                })}
                </tbody>
            </Table>
        }
    }

}

render(
    <BuildSummaryView/>,
    document.getElementById("build-summary-app")
);