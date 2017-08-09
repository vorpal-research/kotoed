import * as React from "react"

interface CommentButtonProps {
    title: string
    icon: string
    onClick: () => void
}

export class CommentButton extends React.Component<CommentButtonProps> {
    output: HTMLPreElement;
    ref: HTMLSpanElement;

    componentDidMount() {
        $(this.ref).tooltip();
    }

    componentDidUpdate() {
        // BS does not like tooltip title to be updated so we have to do this BS.
        $(this.ref).attr('title', this.props.title).tooltip('fixTitle');
    }

    render() {
        return <span ref={(me: HTMLSpanElement) => this.ref = me}
                     className="comment-button"
                     onClick={this.props.onClick}
                     data-toggle="tooltip"
                     data-placement="left"
                     title={this.props.title}>
            <span className={`glyphicon glyphicon-${this.props.icon}`} />
        </span>
    }

}