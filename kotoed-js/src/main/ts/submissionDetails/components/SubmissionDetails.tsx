import * as React from "react";
import {Alert, Button, ButtonToolbar, Row, Table, Well, Label} from "react-bootstrap";
import * as Spinner from "react-spinkit"

import SubmissionHistory from "./SubmissionHistory";
import {SubmissionToRead} from "../../data/submission";
import {Kotoed} from "../../util/kotoed-api";
import {CommentAggregate} from "../../code/remote/comments";
import moment = require("moment");

import "less/submissionDetails.less"
import "less/util.less"

import {makeAggregatesLabel} from "../../code/util/filetree";
import {SubmissionCreate} from "../../submissions/create";
import {DbRecordWrapper} from "../../data/verification";

export interface SubmissionDetailsProps {
    submission: DbRecordWrapper<SubmissionToRead>,
    history: {
        items: Array<SubmissionToRead>
        onMore: (latestId: number) => void
    },
    permissions: {
        changeState: boolean,
        resubmit: boolean
    },
    comments: CommentAggregate
    onResubmit: () => void
    onClose: () => void
    onReopen: () => void
    onMount: () => void
}

export default class SubmissionDetails extends React.Component<SubmissionDetailsProps> {

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

    private renderResultsLink = () => {
        return <a href={Kotoed.UrlPattern.reverse(
            Kotoed.UrlPattern.Submission.Results, {
                id: this.props.submission.record.id
            })
        }>Link</a>;
    };

    private renderReviewLink = () => {
        return <a href={Kotoed.UrlPattern.reverse(
            Kotoed.UrlPattern.CodeReview.Index, {
                id: this.props.submission.record.id
            })
        }>Link</a>;
    };

    private renderLabel = (): JSX.Element | null => {
        switch(this.props.submission.verificationData.status) {
            case "Unknown":
            case "NotReady":
                return <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>
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
                        return <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>
                    default:
                        return null
                }
        }
    };

    private renderResubmit = () => {
        if (this.props.permissions.resubmit)
            return <SubmissionCreate
                onCreate={this.props.onResubmit}
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

    render() {
        return <div>
            <Row>
                <Alert bsStyle="danger"><h1>This page is a sketch with totally dummy data</h1></Alert>
            </Row>
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
                    <h3>{`Submission #${this.props.submission.record.id}`}{" "}{this.renderLabel()}</h3>
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
                                <td className="col-md-9">{this.renderReviewLink()}{" "}{makeAggregatesLabel(this.props.comments)}</td>
                            </tr>
                        </tbody>
                    </Table>
                </Well>
            </Row>
            <Row>
                <h3>Older versions</h3>
                <SubmissionHistory onMore={this.props.history.onMore} items={this.props.history.items}/>
            </Row>
        </div>
    }
}