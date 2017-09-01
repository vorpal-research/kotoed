import * as React from "react";
import {Alert, Button, ButtonToolbar, Row, Table, Well, Label} from "react-bootstrap";
import * as Spinner from "react-spinkit"
import { WithContext as ReactTags } from 'react-tag-input';

import SubmissionHistory from "./SubmissionHistory";
import {SubmissionToRead, Tag} from "../../data/submission";
import {Kotoed} from "../../util/kotoed-api";
import {CommentAggregate} from "../../code/remote/comments";
import moment = require("moment");

import "less/submissionDetails.less"
import "less/util.less"

import {makeAggregatesLabel} from "../../code/util/filetree";
import {SubmissionCreate} from "../../submissions/create";
import {DbRecordWrapper} from "../../data/verification";
import {isSubmissionAvalable} from "../../submissions/util";
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../../util/url";
import {WithId} from "../../data/common";
import SpinnerWithVeil from "../../views/components/SpinnerWithVeil";

export interface SubmissionDetailsProps {
    submission: DbRecordWrapper<SubmissionToRead>,
    loading: boolean,
    history: Array<SubmissionToRead>,
    permissions: {
        changeState: boolean,
        resubmit: boolean
    },
    comments: CommentAggregate,
    tags: Tag[]
}

export interface SubmissionDetailsCallbacks {
    history: {
        onMore: (latestId: number) => void
    },
    onResubmit: (newId: number) => void
    onClose: () => void
    onReopen: () => void
    onMount: () => void
    onTagAdd: (tagName: string) => void
    onTagDelete: (tagIdx: number) => void
}

export default class SubmissionDetails extends React.Component<SubmissionDetailsProps & SubmissionDetailsCallbacks & WithId> {


    private renderResultsLink = (): JSX.Element => {
        if (isSubmissionAvalable({...this.props.submission.record, verificationData: this.props.submission.verificationData}))
            return <a href={makeSubmissionResultsUrl(this.props.submission.record.id)}>Link</a>;
        else
            return <span className={"grayed-out"}>N/A</span>
    };


    private renderReviewList = (): JSX.Element => {
        if (isSubmissionAvalable({...this.props.submission.record, verificationData: this.props.submission.verificationData}))
            return <a href={makeSubmissionReviewUrl(this.props.submission.record.id)}>Link</a>;
        else
            return <span className={"grayed-out"}>N/A</span>
    };


    private renderParentLink = () => {
        if (this.props.submission.record.parentSubmissionId === undefined)
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
                return <Spinner name="circle" color="gray" fadeIn="none" className="display-inline"/>;
            case "Invalid":
                return <Label bsStyle="danger">Invalid</Label>;
            case "Processed":
                switch (this.props.submission.record.state) {
                    case "closed":
                        return <Label bsStyle="default">Closed</Label>;
                    case "invalid":
                        return <Label bsStyle="danger">Invalid</Label>;
                    case "obsolete":
                        return <Label bsStyle="default">Obsolete</Label>;
                    case "open":
                        return <Label bsStyle="success">Open</Label>;
                    case "pending":
                        return <Spinner name="circle" color="gray" fadeIn="none" className="display-inline"/>;
                    default:
                        return null
                }
        }
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
            return <Button bsStyle="danger" bsSize="lg" onClick={this.props.onClose}>Close</Button>;
        else if (this.props.submission.record.state === "closed")
            return <Button bsStyle="success" bsSize="lg"  onClick={this.props.onReopen}>Reopen</Button>;
    };

    private renderTagList = () => {
        return <ReactTags
            tags={this.props.tags}
            handleAddition={this.props.onTagAdd}
            handleDelete={this.props.onTagDelete}
        />
    };

    componentDidMount() {
        this.props.onMount()
    }

    render() {
        if (this.props.loading) {
            return <div style={{
                position: "relative",
                width: "100%",
                height: "500px"
            }}>
                <SpinnerWithVeil/>
            </div>;
        }

        return <div>
            <Row>
                <div className="pull-right">
                    <ButtonToolbar>
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
                        {this.renderTagList()}
                    </h3>
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
                                <td className="col-md-9">{this.renderReviewList()}{" "}{makeAggregatesLabel(this.props.comments)}</td>
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