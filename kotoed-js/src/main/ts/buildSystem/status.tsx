import {render} from "react-dom";
import * as React from "react";
import * as AnsiUp from 'ansi_up';
import {Panel, PanelGroup, Row} from 'react-bootstrap';
import {Kotoed} from "../util/kotoed-api";
import {WithId} from "../data/common";
import {sendAsync} from "../views/components/common";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import * as moment from "../code/components/CommentComponent";
import {poll, SimplePollingStrategy} from "../util/poll";

const ansiToHtml = (() => {
    const ansiUpObject = new AnsiUp.default()
    ansiUpObject.url_whitelist = {}
    return ansiUpObject.ansi_to_html.bind(ansiUpObject) as (txt:string) => string
})()

type BuildStatusState = { loading: true }
                      | { loading: false, status: BuildStatus }

class AnsiComponent extends React.Component<{ contents: string }> {
    constructor(props: { contents: string }) {
        super(props);
    }

    render(): JSX.Element {
        return <div dangerouslySetInnerHTML={{__html: ansiToHtml(this.props.contents)}} />;
    }
}

class BuildStatusView extends React.Component<WithId, BuildStatusState> {
    constructor(props: WithId) {
        super(props);
        this.state = { loading: true };
    }

    tryLoad = async () => {
        const remote = await sendAsync(
            Kotoed.Address.Api.BuildSystem.Build.Status,
            { buildId: this.props.id }
            );
        const newState = { loading: false, status: remote };
        this.setState(newState);
    };

    componentDidMount(): void {
        poll({
            action: this.tryLoad,
            isGoodEnough: () => false,
            strategy: new SimplePollingStrategy({ interval: 1000, shouldGiveUp: () => false })
        })
    }

    bsClassFor = (v: BuildCommandStatus) => {
        switch (v.state) {
            case "FINISHED": return 'success';
            case "RUNNING": return 'info';
            case "WAITING": return 'default';
        }
    };

    render(): JSX.Element {
        if(this.state.loading) return <SpinnerWithVeil />;
        else return <PanelGroup>
            {
                this.state.status.commands.map((v, i) =>
                    <Panel key={`${i}`}
                           collapsible defaultExpanded={false}
                           header={<pre>{v.commandLine}</pre>}
                           bsStyle={this.bsClassFor(v)} >
                        <pre>
                            <AnsiComponent contents={v.cout} />
                            <AnsiComponent contents={v.cerr} />
                        </pre>
                    </Panel>
                )
            }
        </PanelGroup>
    }
}

const params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.BuildSystem.Status, window.location.pathname) || new Map();
const templateId = parseInt(params.get("id"));

render(
    <BuildStatusView id={templateId}/>,
    document.getElementById("build-status-app")
);
