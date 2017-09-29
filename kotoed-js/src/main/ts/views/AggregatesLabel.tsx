import * as React from "react"
import {CommentAggregate} from "../code/remote/comments";
import {OverlayTrigger, Label, Tooltip} from "react-bootstrap";


export default class AggregatesLabel extends React.PureComponent<CommentAggregate> {

    private get labelClass() {
        return this.props.open === 0 ? "default" : "danger"
    }

    private get tooltipText() {
        return `Unresolved comments: ${this.props.open} Resolved comments: ${this.props.closed}`;
    }

    private get number() {
        if (this.props.open == 0)
            return "✔";
        else if (this.props.open >= 100)
            return "∞";
        else
            return `${this.props.open}`
    }


    render() {
        if (this.props.open == 0 && this.props.closed == 0)
            return null;

        return (
            <OverlayTrigger placement="right"
                            overlay={<Tooltip id="comment-aggregates-tooltip">{this.tooltipText}</Tooltip>}>
                <Label className="comments-counter" bsStyle={this.labelClass}>
                    {this.number}
                </Label>
            </OverlayTrigger>
        )
    }
}