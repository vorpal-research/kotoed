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
import {isSubmissionAvalable, renderSubmissionIcon} from "./util";

export type SubmissionWithVer = SubmissionToRead & WithVerificationData


type SubmissionComponentProps = SubmissionWithVer & {
    pendingIsAvailable: boolean
}

export class SubmissionComponent extends React.PureComponent<SubmissionComponentProps> {
    constructor(props: SubmissionComponentProps) {
        super(props);
    }

    private linkToSubDetails = (text: string): JSX.Element => {
        if (isSubmissionAvalable(this.props, this.props.pendingIsAvailable))
            return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: this.props.id})}>{text}</a>;
        else
            return <span className={"grayed-out"}>{text}</span>
    };


    private linkToResults = (): JSX.Element => {
        if (isSubmissionAvalable(this.props, this.props.pendingIsAvailable))
            return <a href={makeSubmissionResultsUrl(this.props.id)}>Results</a>;
        else
            return <span className={"grayed-out"}>Results</span>
    };


    private linkToReview = (): JSX.Element => {
        if (isSubmissionAvalable(this.props, this.props.pendingIsAvailable))
            return <a href={makeSubmissionReviewUrl(this.props.id)}>Review</a>;
        else
            return <span className={"grayed-out"}>Review</span>
    };


    render() {
        return <tr>
            <td>{this.linkToSubDetails(this.props.id.toString())}{" "}{renderSubmissionIcon(this.props, this.props.pendingIsAvailable)}</td>
            <td>{this.linkToSubDetails(moment(this.props.datetime).format('LLLL'))}</td>
            <td>{this.props.revision}</td>
            <td>{this.linkToResults()}</td>
            <td>{this.linkToReview()}</td>
        </tr>
    }

}