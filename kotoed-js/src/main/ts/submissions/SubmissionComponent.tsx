import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger} from "react-bootstrap";
import * as Spinner from "react-spinkit"

import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {SubmissionToRead} from "../data/submission";
import {isStatusFinal, WithVerificationData} from "../data/verification";
import {Kotoed} from "../util/kotoed-api";
import * as moment from "moment";
import {
    isSubmissionAvalable, linkToSubmissionDetails, linkToSubmissionResults, linkToSubmissionReview,
    renderSubmissionIcon, renderSubmissionTags
} from "./util";

export type SubmissionWithVer = SubmissionToRead & WithVerificationData


type SubmissionComponentProps = SubmissionWithVer & {
    pendingIsAvailable: boolean
}

export class SubmissionComponent extends React.PureComponent<SubmissionComponentProps> {
    constructor(props: SubmissionComponentProps) {
        super(props);
    }

    private linkToSubDetails = (text: string): JSX.Element => linkToSubmissionDetails(this.props, text, this.props.pendingIsAvailable);


    private linkToResults = (): JSX.Element => linkToSubmissionResults(this.props, this.props.pendingIsAvailable);


    private linkToReview = (): JSX.Element => linkToSubmissionReview(this.props, this.props.pendingIsAvailable);


    render() {
        return <tr>
            <td>{this.linkToSubDetails(this.props.id.toString())}{" "}{renderSubmissionIcon(this.props, this.props.pendingIsAvailable)}</td>
            <td>{this.linkToSubDetails(moment(this.props.datetime).format('LLLL'))}{" "}{renderSubmissionTags(this.props)}</td>
            <td>{this.props.revision}</td>
            <td>{this.linkToResults()}</td>
            <td>{this.linkToReview()}</td>
        </tr>
    }

}