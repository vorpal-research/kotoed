import * as React from "react";
import * as Spinner from "react-spinkit"
import {OverlayTrigger, Glyphicon, Tooltip, Label} from "react-bootstrap"

import {Submission, SubmissionToRead} from "../data/submission";
import {SubmissionWithVer} from "./SubmissionComponent";

import "sass/util.sass"
import {isStatusFinal} from "../data/verification";
import {Kotoed} from "../util/kotoed-api";
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";
import {intersperse} from "../util/common";
import {Tag} from "../views/components/tags/Tag";
import {SearchCallback} from "../views/components/search";

export function isSubmissionAvalable(sub: SubmissionWithVer, pendingIsAvailable: boolean = false): boolean {
    let {status} = sub.verificationData;
    let {state} = sub;
    switch (status) {
        case "NotReady":
        case "Unknown":
            return pendingIsAvailable;
        case "Invalid":
            return false;
        case "Processed":
            switch (state) {
                case "pending":
                    return pendingIsAvailable;
                case "invalid":
                    return false;
                case "open":
                    return true;
                case "obsolete":
                    return true;
                case "closed":
                    return true;
                default:
                    return false;
            }
    }
}

export function renderSubmissionIcon(sub: SubmissionWithVer, pendingIsAvailable: boolean = false): JSX.Element | null {
    let {status} = sub.verificationData;
    let {state} = sub;
    switch (status) {
        case "NotReady":
        case "Unknown":
            return !pendingIsAvailable ? <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/> : null;
        case "Invalid":
            return <OverlayTrigger placement="right" overlay={<Tooltip id="tooltip">This submission is invalid</Tooltip>}>
                    <span className="text-danger">
                        <Glyphicon glyph="exclamation-sign"/>
                    </span>
                </OverlayTrigger>;
        case "Processed":
            switch (state) {
                case "pending":
                    return !pendingIsAvailable ? <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/> : null;
                case "invalid":
                    return <OverlayTrigger placement="right" overlay={<Tooltip id="tooltip">This submission is invalid</Tooltip>}>
                        <span className="text-danger">
                            <Glyphicon glyph="exclamation-sign"/>
                        </span>
                    </OverlayTrigger>;
                case "open":
                    return null;
                case "obsolete":
                    return null;
                case "closed":
                    return <OverlayTrigger placement="right" overlay={<Tooltip id="tooltip">This submission is closed</Tooltip>}>
                        <span className="grayed-out">
                            <Glyphicon glyph="lock"/>
                        </span>
                    </OverlayTrigger>;
                default:
                    return null;
            }
    }
}

export function linkToSubmissionDetails(submission: SubmissionWithVer,
                                        text?: string,
                                        pendingIsAvailable: boolean = false): JSX.Element {

    text = text || `#${submission.id}`;

    if (isStatusFinal(submission.verificationData.status) || pendingIsAvailable)
        return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: submission.id})}>{text}</a>;
    else
        return <span className={"grayed-out"}>{text}</span>
}


export function linkToSubmissionResults(submission: SubmissionWithVer,
                                        pendingIsAvailable: boolean = false): JSX.Element {
    if (isStatusFinal(submission.verificationData.status) || pendingIsAvailable)
        return <a href={makeSubmissionResultsUrl(submission.id)}>Results</a>;
    else
        return <span className={"grayed-out"}>Results</span>
}


export function linkToSubmissionReview(submission: SubmissionWithVer,
                                       pendingIsAvailable: boolean = false): JSX.Element{
    if (isSubmissionAvalable(submission, pendingIsAvailable))
        return <a href={makeSubmissionReviewUrl(submission.id)}>Review</a>;
    else
        return <span className={"grayed-out"}>Review</span>
}

export function renderSubmissionTags(submission: SubmissionToRead, onClick?: (tag: string) => void): JSX.Element {
    const tags = (submission.submissionTags || []).map(st => st.tag);

    return <span>
        {intersperse<JSX.Element | string>(
            tags.map((t, ix) => <Tag key={`tag-${ix}`} tag={t} removable={false}
                                     onClick={onClick && (() => onClick(t.name))}/>),
            " ")
        }
    </span>
}