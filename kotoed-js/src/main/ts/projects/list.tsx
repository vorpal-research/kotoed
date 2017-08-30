import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"

import {fetchPermissions} from "./remote";
import {
    SearchTableWithVerificationData
} from "../views/components/searchWithVerificationData";
import {WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";
import {ProjectCreate} from "./create";
import {JumboProject} from "../data/submission";

import "less/projects.less"
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {eventBus} from "../eventBus";

type ProjectWithVer = JumboProject & WithVerificationData

class ProjectComponent extends React.PureComponent<ProjectWithVer> {
    constructor(props: ProjectWithVer) {
        super(props);
    }

    linkify = (text: string): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Project.Index, {id: this.props.id})}>{text}</a>;
        else
            return <span className={"grayed-out"}>{text}</span>
    };

    private readonly invalidTooltip = <Tooltip id="tooltip">This project is invalid</Tooltip>;

    private cleanSubmission(submissionId: number) {
        eventBus.awaitOpen().then(_ => {
            return eventBus.send(
                Kotoed.Address.Api.Submission.Verification.Clean,
                {"id": submissionId}
            )
        })
    }

    private renderOpenSubmissions = (): JSX.Element => {
        if (this.props.openSubmissions.length === 0)
            return <span>&mdash;</span>;
        else {
            // Yes, super-minimalistic table
            return <table>
                <tbody>
                    {this.props.openSubmissions.map((sub) => <tr className="roomy-tr" key={`submission-${sub.id}`}>
                        <td><a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: this.props.id})}>{`#${sub.id}`}</a></td>
                        <td><a href={makeSubmissionReviewUrl(sub.id)}>Review</a></td>
                        <td><a href={makeSubmissionResultsUrl(sub.id)}>Results</a></td>
                        <td><a href="#" onClick={_ => this.cleanSubmission(sub.id)}>Clean</a></td>
                        {
                            // TODO: Show only to the teachers
                        }
                    </tr>)}
                </tbody>
            </table>
        }

    };

    private renderIcon = (): JSX.Element | null => {
        let {status} = this.props.verificationData;
        switch (status) {
            case "NotReady":
            case "Unknown":
                return <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>;
            case "Invalid":
                return <OverlayTrigger placement="right" overlay={this.invalidTooltip}>
                    <span className="text-danger">
                        <Glyphicon glyph="exclamation-sign"/>
                    </span>
                </OverlayTrigger>;
            case "Processed":
                return null
        }
    };

    render() {
        return <tr>
            <td>{this.linkify(this.props.id.toString())}</td>
            <td>{this.linkify(this.props.name)}{" "}{this.renderIcon()}</td>
            <td>{this.props.denizen.denizenId}</td>
            <td><a href={this.props.repoUrl}>Link</a></td>
            <td>{this.renderOpenSubmissions()}</td>
        </tr>
    }

}

class ProjectsSearch extends React.Component<{}, {canCreateProject: boolean}> {


    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateProject: false
        };
    }

    componentDidMount() {
        fetchPermissions(id_).then((perms) =>
            this.setState({canCreateProject: perms.createProject})
        );
    }

    toolbarComponent = (redoSearch: () => void) => {
        if (this.state.canCreateProject)
            return <ProjectCreate onCreate={redoSearch} courseId={id_}/>;
        else
            return null;
    };


    renderTable = (children: Array<JSX.Element>): JSX.Element => {
        return (
            <Table striped bordered condensed hover responsive>
                <thead>
                    <tr>
                        <th>Id</th>
                        <th>Name</th>
                        <th>Author</th>
                        <th>Repo</th>
                        <th>Open submissions</th>
                    </tr>
                </thead>
                <tbody>
                    {children}
                </tbody>
            </Table>)
    };

    render() {
        return (
            <SearchTableWithVerificationData
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Project.SearchForCourse}
                countAddress={Kotoed.Address.Api.Project.SearchForCourseCount}
                makeBaseQuery={() => {
                    return {
                        find: {
                            courseId: id_
                        }
                    }
                }}
                wrapResults={this.renderTable}
                elementComponent={(key, c: ProjectWithVer) => <ProjectComponent {...c} key={key} />}
                toolbarComponent={this.toolbarComponent}
            />
        );
    }
}

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Course.Index, window.location.pathname);
if (params == null) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

let id = params.get("id");

if (id === undefined ) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

let id_ = parseInt(id);

if (isNaN(id_)) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

render(
    <ProjectsSearch/>,
    document.getElementById('project-search-app')
);
