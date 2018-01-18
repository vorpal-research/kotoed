import * as React from "react";
import {Alert, Button, ButtonToolbar, OverlayTrigger, Popover, Row, Table, Well, Label} from "react-bootstrap";
import * as Spinner from "react-spinkit"

import SubmissionHistory from "./SubmissionHistory";
import {BloatSubmission, SubmissionToRead, Tag} from "../../data/submission";
import {Kotoed} from "../../util/kotoed-api";
import {CommentAggregate} from "../../code/remote/comments";
import moment = require("moment");

import "sass/submissionDetails.sass"
import "sass/util.sass"

import {SubmissionCreate} from "../../submissions/create";
import {DbRecordWrapper, isStatusFinal, WithVerificationData} from "../../data/verification";
import {isSubmissionAvalable} from "../../submissions/util";
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../../util/url";
import {WithId} from "../../data/common";
import SpinnerWithVeil, {SpinnerWithBigVeil} from "../../views/components/SpinnerWithVeil";
import VerificationDataAlert from "../../views/components/VerificationDataAlert";
import {makeProfileLink} from "../../util/denizen";
import * as Autosuggest from "react-autosuggest";
import {SimpleAutoSuggest} from "../../views/components/tags/SimpleAutosuggest";
import {Tagger} from "../../views/components/tags/Tagger";
import AggregatesLabel from "../../views/AggregatesLabel";

export interface SubmissionDetailsProps {
    submission: DbRecordWrapper<BloatSubmission>,
    loading: boolean,
    history: Array<SubmissionToRead>,
    permissions: {
        changeState: boolean,
        resubmit: boolean,
        clean: boolean,
        tags: boolean
    },
    comments: CommentAggregate,
    tags: Tag[],
    availableTags: Tag[]
    tagsDisabled: boolean
}

export interface SubmissionDetailsCallbacks {
    history: {
        onMore: (latestId: number) => void
    },
    onResubmit: (newId: number) => void
    onClose: () => void
    onReopen: () => void
    onClean: () => void
    onMount: () => void
    onDelete: () => void
    onTagAdd: (tagId: number) => void
    onTagDelete: (tagId: number) => void
}

export default class SubmissionDetails extends React.Component<SubmissionDetailsProps & SubmissionDetailsCallbacks & WithId> {


    private renderResultsLink = (): JSX.Element => {
        if (isStatusFinal(this.props.submission.verificationData.status))
            return <a href={makeSubmissionResultsUrl(this.props.submission.record.id)}>Link</a>;
        else
            return <span className={"grayed-out"}>N/A</span>
    };


    private renderReviewList = (): JSX.Element => {
        if (isSubmissionAvalable({
                ...this.props.submission.record as SubmissionToRead,
                verificationData: this.props.submission.verificationData
        }))
            return <a href={makeSubmissionReviewUrl(this.props.submission.record.id)}>Link</a>;
        else
            return <span className={"grayed-out"}>N/A</span>
    };


    private renderParentLink = () => {
        if (!this.props.submission.record.parentSubmissionId)
            return <span>&mdash;</span>;
        return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {
            id: this.props.submission.record.parentSubmissionId
        })}>Link</a>;
    };

    private renderProjectLink = () => {
        return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Project.Index, {
            id: this.props.submission.record.projectId
        })}>Link</a>;
    };

    private renderLabel = (): JSX.Element | null => {
        switch(this.props.submission.verificationData.status) {
            case "Unknown":
            case "NotReady":
                return <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>;
            case "Invalid":
                return <Label bsStyle="default">Invalid</Label>;
            case "Processed":
                switch (this.props.submission.record.state) {
                    case "closed":
                        return <Label bsStyle="danger">Closed</Label>;
                    case "invalid":
                        return <Label bsStyle="danger">Invalid</Label>;
                    case "obsolete":
                        return <Label bsStyle="default">Obsolete</Label>;
                    case "open":
                        return <Label bsStyle="success">Open</Label>;
                    case "pending":
                        return <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>;
                    default:
                        return null
                }
        }
    };

    private renderClean = () => {
        if (this.props.permissions.clean)
            return <Button bsStyle="primary" onClick={() => this.props.onClean()}>Clean</Button>;
        else
            return null;
    };

    private renderResubmit = () => {
        if (this.props.permissions.resubmit)
            return <SubmissionCreate
                onCreate={(id) => this.props.onResubmit(id)}
                projectId={this.props.submission.record.projectId}
                parentSubmission={this.props.submission.record.id}/>;
        else
            return null;
    };

    private renderStateChange = () => {
        if (!this.props.permissions.changeState)
            return null;
        else if (this.props.submission.record.state === "open")
            return <Button bsStyle="danger" onClick={this.props.onClose}>Close</Button>;
        else if (this.props.submission.record.state === "closed")
            return <Button bsStyle="success" onClick={this.props.onReopen}>Reopen</Button>;
    };

    private renderDelete = () => {
        if (!this.props.permissions.changeState)  // Separate permission would be nice, but I am too lazy
            return null;
        else if (this.props.submission.record.state != "open" &&
                this.props.submission.record.state != "closed" &&
                this.props.submission.record.state != "invalid")
            return null;
        else
            return <OverlayTrigger trigger="click" placement="bottom" overlay={
                <Popover id="can-delete-popover">
                    <strong>Are you sure?</strong>
                    <br />
                    <Button bsStyle="danger"
                            block
                            onClick={this.props.onDelete}>
                        Delete
                    </Button>
                </Popover>
            }>
                <Button bsStyle="link">
                    <span className={"text-danger"}>Delete this submission</span>
                </Button>
            </OverlayTrigger>
    };

    private onTagAdd = (tagId: number) => {
        this.props.onTagAdd(tagId)
    };

    private onTagDelete = (tagId: number) => {
        return this.props.onTagDelete(tagId)
    };

    private renderTagList = () => {
        return this.props.permissions.tags && <Tagger
            onTagAdd={tag => this.onTagAdd(tag.id)}
            onTagRemove={tag => this.onTagDelete(tag.id)}
            currentTags={this.props.tags}
            availableTags={this.props.availableTags}
            disabled={this.props.tagsDisabled}
            classNames={{
                inputWrapper: "col-md-3",
                tagsWrapper: "col-md-9 tags-container",
                wrapper: "row tagger"
            }}
        />
    };

    componentDidMount() {
        this.props.onMount()
    }

    render() {
        if (this.props.loading) {
            return <SpinnerWithBigVeil/>;
        }

        return <div>
            <Row>
                {/*TODO add give up handling*/}
                <VerificationDataAlert
                    makeString={(obj: DbRecordWrapper<SubmissionToRead>) => `Submission #${obj.record.id}`}
                    obj={this.props.submission} gaveUp={false}/>
            </Row>
            <Row>
                <div className="pull-right">
                    <ButtonToolbar>
                        {this.renderDelete()}
                        {this.renderClean()}
                        {this.renderStateChange()}
                        {this.renderResubmit()}
                    </ButtonToolbar>
                    <div className="clearfix"/>
                </div>
            </Row>
            <Row>
                <div className="vspace-10"/>
            </Row>

            <Row>
                <Well>
                    <h3>
                        {`Submission #${this.props.submission.record.id}`}
                        {" "}
                        {this.renderLabel()}
                    </h3>
                    {this.renderTagList()}
                    <Table className="submission-details">
                        <tbody>
                            <tr>
                                <td className="col-md-3">Created at</td>
                                <td className="col-md-9">{moment(this.props.submission.record.datetime).format('LLLL')}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">State</td>
                                <td className="col-md-9">{this.props.submission.record.state}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Repo</td>
                                <td className="col-md-9"><a href={this.props.submission.record.project.repoUrl}>Link</a></td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Author</td>
                                <td className="col-md-9">{makeProfileLink(this.props.submission.record.project.denizen)}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Project</td>
                                <td className="col-md-9">{this.renderProjectLink()}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">VCS revision</td>
                                <td className="col-md-9">{this.props.submission.record.revision}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Parent submission</td>
                                <td className="col-md-9">{this.renderParentLink()}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Results</td>
                                <td className="col-md-9">{this.renderResultsLink()}</td>
                            </tr>
                            <tr>
                                <td className="col-md-3">Review</td>
                                <td className="col-md-9">
                                    {this.renderReviewList()}
                                    {" "}
                                    <AggregatesLabel {...this.props.comments}/>
                                </td>
                            </tr>
                        </tbody>
                    </Table>
                </Well>
            </Row>
            {this.props.history.length > 0 && <Row>
                <h3>Older versions</h3>
                {/*TODO Verification data here???*/}
                <SubmissionHistory onMore={this.props.history.onMore} items={this.props.history}/>
            </Row>}
        </div>
    }
}