import * as React from "react"
import * as Spinner from "react-spinkit"
import {Alert, Glyphicon} from "react-bootstrap";

import "less/util.less"
import "react-spinkit/css/base.css"
import "react-spinkit/css/loaders-css.css"
import {DbRecordWrapper} from "../../data/verification";


export interface VerificationDataAlertProps<T> {
    makeString: (obj: DbRecordWrapper<T>) => string
    obj: DbRecordWrapper<T>
    gaveUp: boolean
}

export default class VerificationDataAlert<T> extends React.Component<VerificationDataAlertProps<T>> {
    getStyle = () => {
        switch(this.props.obj.verificationData.status) {
            case "Invalid":
                return "danger";
            default:
                return undefined;
        }
    };

    renderText = () => {
        switch(this.props.obj.verificationData.status) {
            case "Invalid":
                return <span>
                    <Glyphicon glyph="exclamation-sign"/>
                    {` ${this.props.makeString(this.props.obj)} is invalid`}
                </span>;
            case "NotReady":
            case "Unknown":
                if (this.props.gaveUp) {
                    return <span>
                        {` ${this.props.makeString(this.props.obj)} is being processed and this page does not wait
                        for it anymore. Try reloading the page`}
                    </span>
                } else {
                    return <span>
                        <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>
                            {` ${this.props.makeString(this.props.obj)} is being processed. Please wait...`}
                        </span>
                }
            default:
                return null;
        }
    };

    render() {
        if (this.props.obj.verificationData.status !== "Processed")
            return <Alert bsStyle={this.getStyle()}>{this.renderText()}</Alert>;
        else
            return null;
    }
}