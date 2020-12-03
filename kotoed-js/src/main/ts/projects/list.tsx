import * as React from "react";
import {
    Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger, Row, Button, Col
} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"

import {fetchPermissions} from "./remote";
import {DbRecordWrapper, isStatusFinal, WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";
import {ProjectCreate} from "./create";
import {JumboProject, SubmissionToRead, Tag} from "../data/submission";

import "less/projects.less"
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {eventBus} from "../eventBus";
import {CourseToRead} from "../data/course";
import {SpinnerWithBigVeil} from "../views/components/SpinnerWithVeil";
import VerificationDataAlert from "../views/components/VerificationDataAlert";
import {Tag as TagComponent} from "../views/components/tags/Tag"
import {pollDespairing} from "../util/poll";
import {truncateString} from "../util/string";
import {fetchCourse} from "../courses/remote";
import {ChoosyByVerDataSearchTable, ChoosySearchTable, SearchCallback} from "../views/components/search";
import {
    linkToSubmissionDetails, linkToSubmissionResults, linkToSubmissionReview,
    renderSubmissionIcon, renderSubmissionTags
} from "../submissions/util";
import {BloatDenizen, Denizen} from "../data/denizen";
import {makeFullName, makeGroup, makeProfileLink, makeRealName} from "../util/denizen";
import UrlPattern = Kotoed.UrlPattern;
import * as ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import {sendAsync} from "../views/components/common";
import {fetchAvailableTags} from "../submissionDetails/remote";
import {intersperse} from "../util/common";

type ProjectWithVer = JumboProject & WithVerificationData & { cb: SearchCallback }

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
            return sendAsync(
                Kotoed.Address.Api.Submission.Verification.Clean,
                {"id": submissionId}
            )
        })
    }

    handleSearchableClick = (word: string) => {
        const searchState = this.props.cb();
        // XXX: we can do better
        if (!searchState.oldKey.includes(word)) {
            searchState.toggleSearch(searchState.oldKey + " " + word)
        }
    };

    private renderPermanentAdjustment = (): JSX.Element => {
        const adj = this.props.permanentAdjustment
        return <span>{adj != null && adj != 0 &&
        <p>Permanent adjustment {adj} by
            {(this.props.permanentAdjustmentSubmissions || [])
                .map((sub) =>
                    <span> {linkToSubmissionDetails(sub)}</span>
                )
            }</p>}
        </span>
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
                    <td>{renderSubmissionTags(sub, this.handleSearchableClick)}</td>
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

    private renderRealName = (denizen: BloatDenizen): JSX.Element => {
        let fullName = makeFullName(denizen);
        let group = makeGroup(denizen);
        let nameLink =
            <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Profile.Index, {id: denizen.id})}>
                {fullName || `#${denizen.id}`}
            </a>;
        let groupLink = group &&
            <a style={{cursor: "pointer"}} onClick={() => this.handleSearchableClick(group)}>{group}</a>;

        return <span>
            ({nameLink}{groupLink && <span>, {groupLink}</span> || null})
        </span>
    };

    private renderProfileLinks = (denizen: BloatDenizen): JSX.Element => {
        return <span>
            <a style={{cursor: "pointer"}}
               onClick={() => this.handleSearchableClick(denizen.denizenId)}>
                {truncateString(denizen.denizenId, 16)}
            </a>
            {" "}{this.renderRealName(denizen)}
        </span>
    };

    render() {
        return <tr>
            <td>{this.linkify(this.props.id.toString())}</td>
            <td>{this.linkify(truncateString(this.props.name || "", 30))}{" "}{this.renderIcon()}</td>
            <td>{this.renderProfileLinks(this.props.denizen)}</td>
            <td><a href={this.props.repoUrl}>Link</a></td>
            <td>{this.renderPermanentAdjustment()}{this.renderOpenSubmissions()}</td>
        </tr>
    }

}

interface ProjectSearchState {
    canCreateProject: boolean,
    canEditCourse: boolean,
    course?: DbRecordWrapper<CourseToRead>
    tags: Array<Tag>
}

class ProjectsSearchTable extends ChoosyByVerDataSearchTable<JumboProject & WithVerificationData,
    { withVerificationData: true, find: { courseId: number } }> {

    protected isGoodEnough(data: (JumboProject & WithVerificationData)[]) {
        return super.isGoodEnough(data) &&
            data.every((datum: JumboProject & WithVerificationData) => {
                return datum.openSubmissions.every((sub: SubmissionToRead & WithVerificationData) =>
                    isStatusFinal(sub.verificationData.status))
            })
    }
}

class ProjectsSearch extends React.Component<{}, ProjectSearchState> {
    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateProject: false,
            canEditCourse: false,
            tags: []
        };
    }

    async componentDidMount() {

        const processCourse = (course: DbRecordWrapper<CourseToRead>) => {
            this.setState({
                course
            })
        };

        await pollDespairing({
            action: async () => await fetchCourse(id_),
            isGoodEnough: ((course: DbRecordWrapper<CourseToRead>) => isStatusFinal(course.verificationData.status)),
            onIntermediate: processCourse,
            onGiveUp: processCourse,
            onFinal: processCourse
        });
        const perms = await fetchPermissions(id_);
        let tags: Array<Tag>
        if (perms.viewTags)
            tags = await fetchAvailableTags();
        else
            tags = []
        this.setState({canCreateProject: perms.createProject, canEditCourse: perms.editCourse, tags})
    }

    toolbarButtons = (cb: SearchCallback) => {
        return <ButtonToolbar>
            {this.state.canEditCourse &&
            <Button bsStyle={"link"} href={UrlPattern.reverse(UrlPattern.Course.Edit, {id: id_})}>
                Edit course
            </Button>
            }
            {this.state.canCreateProject &&
            <ProjectCreate onCreate={() => {
                const search = cb();
                search.toggleSearch(search.oldKey)
            }} courseId={id_}/>
            }
        </ButtonToolbar>;
    };

    tagCloug = (cb: SearchCallback) => {
        return this.state.tags.length !== 0 && <Row>
            <Col xs={12} sm={12} md={12} lg={12}>
                {intersperse<JSX.Element | string>(this.state.tags.map(tag => <TagComponent key={tag.id} tag={tag}
                                                                                            removable={false}
                                                                                            onClick={() => {
                                                                                                const searchState = cb();
                                                                                                // XXX: we can do better
                                                                                                // XXX: also it's a copy-paste from ProjectComponent
                                                                                                if (!searchState.oldKey.includes(tag.name)) {
                                                                                                    searchState.toggleSearch(searchState.oldKey + " " + tag.name)
                                                                                                }
                                                                                            }}/>), " ")}
            </Col>
        </Row>;
    };

    toolbar = (cb: SearchCallback) => {
        return <div>
            <Row>
                <Col xs={12} sm={12} md={12} lg={12}>
                    <div className="search-toolbar">
                        <div className="pull-right">
                            {this.toolbarButtons(cb)}
                        </div>
                        <div className="clearfix"/>
                        <div className="vspace-10"/>
                    </div>
                </Col>
            </Row>
            <Row className="tag-cloud">
                <Col xs={12} sm={12} md={12} lg={12}>
                    {this.tagCloug(cb)}
                </Col>
            </Row>
        </div>
    }

    renderTable = (children: Array<JSX.Element>): JSX.Element => {
        return (
            <Table striped bordered condensed hover responsive>
                <thead>
                <tr>
                    <th className="col-md-1">Id</th>
                    <th className="col-md-2">Name</th>
                    <th className="col-md-3">Author</th>
                    <th className="col-md-1">Repo</th>
                    <th className="col-md-5">Open submissions</th>
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
                        elementComponent={(key, c: ProjectWithVer, cb: SearchCallback) => <ProjectComponent {...c}
                                                                                                            key={key}
                                                                                                            cb={cb}/>}
                        toolbarComponent={this.toolbar}
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

if (id === undefined) {
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
