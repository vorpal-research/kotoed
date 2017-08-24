import * as React from "react"
import {Glyphicon, Tooltip, OverlayTrigger} from "react-bootstrap";

interface CommentButtonProps {
    title: string
    icon: string
    onClick: () => void
}

export class CommentButton extends React.Component<CommentButtonProps> {
    makeTooltip = () => {
        return <Tooltip id="tooltip">{this.props.title}</Tooltip>
    };


    render() {
        return (
            <OverlayTrigger placement="left" overlay={this.makeTooltip()}>
                <span
                     className="comment-button"
                     onClick={this.props.onClick}>
                    <Glyphicon glyph={this.props.icon} />
                </span>
            </OverlayTrigger>)
    }

}