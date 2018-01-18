import * as React from "react";
import {Row, ButtonToolbar, Button, OverlayTrigger, Tooltip} from "react-bootstrap";

import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {fetchPermissions, fetchProject} from "./remote";
import snafuDialog from "../util/snafuDialog";
import {renderSubmissionTable} from "./table";
import {SubmissionComponent, SubmissionWithVer} from "./SubmissionComponent";
import {SubmissionCreate} from "./create";
import {DbRecordWrapper, isStatusFinal} from "../data/verification";
import {CourseToRead} from "../data/course";
import {SpinnerWithBigVeil} from "../views/components/SpinnerWithVeil";
import VerificationDataAlert from "../views/components/VerificationDataAlert";
import {ProjectToRead} from "../data/project";
import {pollDespairing} from "../util/poll";
import {ChoosyByVerDataSearchTable} from "../views/components/search";
import {sendAsync} from "../views/components/common";
import Address = Kotoed.Address;
import * as Popover from "react-bootstrap/lib/Popover";

interface SubmissionListState {
    canCreateSubmission: boolean
    canDeleteProject: boolean
    project?: DbRecordWrapper<ProjectToRead>
}

class SubmissionList extends React.Component<{}, SubmissionListState> {


    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateSubmission: false,
            canDeleteProject: false
        };
    }

    componentDidMount() {
        const processProject = (project: DbRecordWrapper<ProjectToRead>) => {
            this.setState({
                project
            })
        };

        pollDespairing({
            action: async () => await fetchProject(id_),
            isGoodEnough: ((proj: DbRecordWrapper<ProjectToRead>) => isStatusFinal(proj.verificationData.status)),
            onIntermediate: processProject,
            onGiveUp: processProject,
            onFinal: processProject
        }).then(() => {
            return fetchPermissions(id_)
        }).then((perms) =>
            this.setState({
                canCreateSubmission: perms.createSubmission,
                canDeleteProject: perms.deleteProject
            })
        )
    }

    deleteProject = async () => {
        if(this.state.project) {
            await sendAsync(Address.Api.Project.Delete, { id: this.state.project.record.id });
            window.location.href = Kotoed.UrlPattern.reverse(
                Kotoed.UrlPattern.Course.Index,
                { id: this.state.project.record.courseId }
            )
        }

    };

    toolbarComponent = () => {
        return <ButtonToolbar>
            {this.state.project &&
                <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">New tab</Tooltip>}>
                    <Button bsStyle="link"
                            href={this.state.project.record.repoUrl}
                            target="_blank">
                        Go to repo
                    </Button>
                </OverlayTrigger>}
            {
                this.state.project &&
                this.state.canDeleteProject &&
                    <OverlayTrigger trigger="click" placement="bottom" overlay={
                        <Popover id="can-delete-popover">
                            <strong>Are you sure?</strong>
                            <br />
                            <Button bsStyle="danger"
                                    block
                                    onClick={this.deleteProject}>
                                Delete
                            </Button>
                        </Popover>
                    }>
                        <Button bsStyle="link">
                            <span className={"text-danger"}>Delete this project</span>
                        </Button>
                    </OverlayTrigger>
                }
            {this.state.canCreateSubmission && <SubmissionCreate onCreate={(newId) =>
                window.location.href =
                    Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: newId})
            } projectId={id_}/>}
        </ButtonToolbar>
    };

    render() {
        if (!this.state.project)
            return <SpinnerWithBigVeil/>;
        return (
            <div>
                <Row>
                    {/*TODO add give up handling*/}
                    <VerificationDataAlert
                        makeString={(obj: DbRecordWrapper<CourseToRead>) => `Project "${obj.record.name}"`}
                        obj={this.state.project} gaveUp={false}/>
                </Row>
                <Row>
                    <ChoosyByVerDataSearchTable
                        shouldPerformInitialSearch={() => true}
                        searchAddress={Kotoed.Address.Api.Submission.List}
                        countAddress={Kotoed.Address.Api.Submission.ListCount}
                        withSearch={false}
                        makeBaseQuery={() => {
                            return {
                                withVerificationData: true,
                                find: {
                                    projectId: id_,
                                    stateIn: [
                                        "pending", "invalid", "open", "closed"
                                    ]
                                }
                            }
                        }}
                        wrapResults={renderSubmissionTable}
                        elementComponent={(key, c: SubmissionWithVer) =>
                            <SubmissionComponent {...c}
                                                 key={key}
                                                 pendingIsAvailable={false}/>}
                        toolbarComponent={this.toolbarComponent}
                    />
                </Row>
            </div>
        );
    }
}

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Project.Index, window.location.pathname);

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
    <SubmissionList/>,
    document.getElementById('submission-list-app')
);
