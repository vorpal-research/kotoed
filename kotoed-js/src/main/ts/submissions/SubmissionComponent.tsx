import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger} from "react-bootstrap";
import * as Spinner from "react-spinkit"

import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {SubmissionToRead} from "../data/submission";
import {WithVerificationData} from "../data/verification";
import {Kotoed} from "../util/kotoed-api";
import * as moment from "moment";

export type SubmissionWithVer = SubmissionToRead & WithVerificationData


export class SubmissionComponent extends React.PureComponent<SubmissionWithVer> {
    constructor(props: SubmissionWithVer) {
        super(props);
    }

    private linkToSubDetails = (text: string): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: this.props.id})}>{text}</a>;
        else
            return <span className={"grayed-out"}>{text}</span>
    };


    private linkToResults = (): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={makeSubmissionResultsUrl(this.props.id)}>Results</a>;
        else
            return <span className={"grayed-out"}>Results</span>
    };


    private linkToReview = (): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={makeSubmissionReviewUrl(this.props.id)}>Review</a>;
        else
            return <span className={"grayed-out"}>Review</span>
    };


    private readonly invalidTooltip = <Tooltip id="tooltip">This submission is invalid</Tooltip>;
    private readonly closedTooltip = <Tooltip id="tooltip">This submission is closed</Tooltip>;

    private readonly spinner =  <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>;
    private readonly exclamation = <OverlayTrigger placement="right" overlay={this.invalidTooltip}>
        <span className="text-danger">
            <Glyphicon glyph="exclamation-sign"/>
        </span>
    </OverlayTrigger>;
    private readonly lock = <OverlayTrigger placement="right" overlay={this.closedTooltip}>
        <span className="text-danger">
            <Glyphicon glyph="lock"/>
        </span>
    </OverlayTrigger>;


    private renderIcon = (): JSX.Element | null => {
        let {status} = this.props.verificationData;
        let {state} = this.props;
        switch (status) {
            case "NotReady":
            case "Unknown":
                return this.spinner;
            case "Invalid":
                return this.exclamation;
            case "Processed":
                switch (state) {
                    case "pending":
                        return this.spinner;
                    case "invalid":
                        return this.exclamation;
                    case "open":
                        return null;
                    case "obsolete":
                        return null; // Should not happen here
                    case "closed":
                        return this.lock;
                    default:
                        return null;
                }
        }
    };
    render() {
        return <tr>
            <td>{this.linkToSubDetails(this.props.id.toString())}{" "}{this.renderIcon()}</td>
            <td>{this.linkToSubDetails(moment(this.props.datetime).format('LLLL'))}</td>
            <td>{this.props.revision}</td>
            <td>{this.linkToResults()}</td>
            <td>{this.linkToReview()}</td>
        </tr>
    }

}