import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger, Row} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"

import {fetchPermissions} from "./remote";
import {DbRecordWrapper, isStatusFinal, WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";
import {ProjectCreate} from "./create";
import {JumboProject, SubmissionToRead} from "../data/submission";

import "less/projects.less"
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {eventBus} from "../eventBus";
import {CourseToRead} from "../data/course";
import {SpinnerWithBigVeil} from "../views/components/SpinnerWithVeil";
import VerificationDataAlert from "../views/components/VerificationDataAlert";
import {pollDespairing} from "../util/poll";
import {truncateString} from "../util/string";
import {fetchCourse} from "../courses/remote";
import {ChoosyByVerDataSearchTable, ChoosySearchTable} from "../views/components/search";
import {
    linkToSubmissionDetails, linkToSubmissionResults, linkToSubmissionReview,
    renderSubmissionIcon
} from "../submissions/util";
import {Denizen} from "../data/denizen";

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

    profileLink = (denizen: Denizen): JSX.Element => {
        return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Profile.Index, {id: denizen.id})}>
            {truncateString(this.props.denizen.denizenId, 30)}
            </a>
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
                        <td>{linkToSubmissionDetails(sub)}</td>
                        <td>{linkToSubmissionResults(sub)}</td>
                        <td>{linkToSubmissionReview(sub)}</td>
                        <td>{renderSubmissionIcon(sub)}</td>
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
            <td>{this.linkify(truncateString(this.props.name, 30))}{" "}{this.renderIcon()}</td>
            <td>{this.profileLink(this.props.denizen)}</td>
            <td><a href={this.props.repoUrl}>Link</a></td>
            <td>{this.renderOpenSubmissions()}</td>
        </tr>
    }

}

interface ProjectSearchProps {
    canCreateProject: boolean,
    course?: DbRecordWrapper<CourseToRead>
}

class ProjectsSearchTable extends ChoosyByVerDataSearchTable<JumboProject & WithVerificationData,
        {withVerificationData: true, find: {courseId: number }}> {

    protected isGoodEnough(data: (JumboProject & WithVerificationData)[]) {
        return super.isGoodEnough(data) &&
            data.every((datum: JumboProject & WithVerificationData) => {
                return datum.openSubmissions.every((sub: SubmissionToRead & WithVerificationData) =>
                    isStatusFinal(sub.verificationData.status))
            })
    }
}

class ProjectsSearch extends React.Component<{}, ProjectSearchProps> {
    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateProject: false
        };
    }

    componentDidMount() {

        const processCourse = (course: DbRecordWrapper<CourseToRead>) => {
            this.setState({
                course
            })
        };

        pollDespairing({
            action: async () => await fetchCourse(id_),
            isGoodEnough: ((course: DbRecordWrapper<CourseToRead>) => isStatusFinal(course.verificationData.status)),
            onIntermediate: processCourse,
            onGiveUp: processCourse,
            onFinal: processCourse
        }).then(() => {
            return fetchPermissions(id_)
        }).then((perms) =>
            this.setState({canCreateProject: perms.createProject})
        )
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
        if (!this.state.course)
            return <SpinnerWithBigVeil/>;
        return (
            <div>
                <Row>
                    {/*TODO add give up handling*/}
                    <VerificationDataAlert
                        makeString={(obj: DbRecordWrapper<CourseToRead>) => `Course "${obj.record.name}"`}
                        obj={this.state.course} gaveUp={false}/>
                </Row>
                <Row>
                    <ProjectsSearchTable
                        shouldPerformInitialSearch={() => true}
                        searchAddress={Kotoed.Address.Api.Project.SearchForCourse}
                        countAddress={Kotoed.Address.Api.Project.SearchForCourseCount}
                        makeBaseQuery={() => {
                            return {
                                withVerificationData: true,
                                find: {
                                    courseId: id_
                                }
                            }
                        }}
                        wrapResults={this.renderTable}
                        elementComponent={(key, c: ProjectWithVer) => <ProjectComponent {...c} key={key} />}
                        toolbarComponent={this.toolbarComponent}
                    />
                </Row>
            </div>
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
