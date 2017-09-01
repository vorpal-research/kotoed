import * as React from "react";
import {Row} from "react-bootstrap";

import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {fetchPermissions, fetchProject} from "./remote";
import {
    isStatusFinal,
    SearchTableWithVerificationData
} from "../views/components/searchWithVerificationData";
import snafuDialog from "../util/snafuDialog";
import "less/projects.less"
import {renderSubmissionTable} from "./table";
import {SubmissionComponent, SubmissionWithVer} from "./SubmissionComponent";
import {SubmissionCreate} from "./create";
import {DbRecordWrapper} from "../data/verification";
import {CourseToRead} from "../data/course";
import {SpinnerWithBigVeil} from "../views/components/SpinnerWithVeil";
import VerificationDataAlert from "../views/components/VerificationDataAlert";
import {ProjectToRead} from "../data/project";
import {pollDespairing} from "../util/poll";

interface SubmissionListState {
    canCreateSubmission: boolean
    project?: DbRecordWrapper<ProjectToRead>
}

class SubmissionList extends React.Component<{}, SubmissionListState> {


    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateSubmission: false
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
            isGoodEnough: ((course: DbRecordWrapper<CourseToRead>) => isStatusFinal(course.verificationData.status)),
            onIntermediate: processProject,
            onGiveUp: processProject,
            onFinal: processProject
        }).then(() => {
            return fetchPermissions(id_)
        }).then((perms) =>
            this.setState({canCreateSubmission: perms.createSubmission})
        )
    }

    toolbarComponent = (redoSearch: () => void) => {
        if (this.state.canCreateSubmission)
            return <SubmissionCreate onCreate={redoSearch} projectId={id_}/>;
        else
            return null;
    };

    render() {
        if (!this.state.project)
            return <SpinnerWithBigVeil/>;
        return (
            <div>
                <Row>
                    {/*TODO add give up handling*/}
                    <VerificationDataAlert
                        makeString={(obj: DbRecordWrapper<CourseToRead>) => `Course #${obj.record.id}`}
                        obj={this.state.project} gaveUp={false}/>
                </Row>
                <Row>
                    <SearchTableWithVerificationData
                        shouldPerformInitialSearch={() => true}
                        searchAddress={Kotoed.Address.Api.Submission.List}
                        countAddress={Kotoed.Address.Api.Submission.ListCount}
                        withSearch={false}
                        makeBaseQuery={() => {
                            return {
                                find: {
                                    projectId: id_
                                }
                            }
                        }}
                        wrapResults={renderSubmissionTable}
                        elementComponent={(key, c: SubmissionWithVer) => <SubmissionComponent {...c} key={key} pendingIsAvailable={false}/>}
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
