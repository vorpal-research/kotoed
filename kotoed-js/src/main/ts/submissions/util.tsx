import * as React from "react";
import * as Spinner from "react-spinkit"
import {OverlayTrigger, Glyphicon, Tooltip} from "react-bootstrap"

import {Submission} from "../data/submission";
import {SubmissionWithVer} from "./SubmissionComponent";

import "less/util.less"

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
};

export function renderSubmissionIcon(sub: SubmissionWithVer, pendingIsAvailable: boolean = false): JSX.Element | null {
    let {status} = sub.verificationData;
    let {state} = sub;
    switch (status) {
        case "NotReady":
        case "Unknown":
            return pendingIsAvailable ? <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/> : null;
        case "Invalid":
            return <OverlayTrigger placement="right" overlay={<Tooltip id="tooltip">This submission is invalid</Tooltip>}>
                    <span className="text-danger">
                        <Glyphicon glyph="exclamation-sign"/>
                    </span>
                </OverlayTrigger>;
        case "Processed":
            switch (state) {
                case "pending":
                    return pendingIsAvailable ? <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/> : null;
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
};