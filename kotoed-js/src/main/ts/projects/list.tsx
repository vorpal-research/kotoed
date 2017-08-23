import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal, Table} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"

import {eventBus, isSnafu, SoftError} from "../eventBus";
import {fetchPermissions} from "./remote";
import {
    SearchTableWithVerificationData
} from "../views/components/searchWithVerificationData";
import {JumboProject, Project} from "../data/project";
import {WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";

type ProjectWithVer = JumboProject & WithVerificationData

class ProjectComponent extends React.PureComponent<ProjectWithVer> {
    constructor(props: ProjectWithVer) {
        super(props);
    }

    linkify = (text: string): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={Kotoed.UrlPattern.NotImplemented}>{text}</a>;
        else
            return <span className={"grayed-out"}>{text}</span>
    };

    renderIcon = (): JSX.Element | null => {
        let {status} = this.props.verificationData;
        switch (status) {
            case "NotReady":
            case "Unknown":
                return <Spinner name="three-bounce" color="green" fadeIn="none"/>;
            case "Invalid":
                return <span className="glyphicon glyphicon-exclamation-sign text-danger"/>;
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
            <td>{this.linkify("TODO")}</td>
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
            return null; // TODO
        else
            return null;
    };


    renderTable = (children: Array<JSX.Element>): JSX.Element => {
        return (
            <Table responsive>
                <thead>
                    <tr>
                        <th>Id</th>
                        <th>Name</th>
                        <th>Author</th>
                        <th>Repo</th>
                        <th>Last submission</th>
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
