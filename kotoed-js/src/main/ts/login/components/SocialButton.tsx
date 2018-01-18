import * as React from "react"

import "sass/bootstrap-social.sass"

import "font-awesome/less/font-awesome.less"


interface SocialButtonProps {
    provider: string
    onClick: (provider: string) => void
}

export default class SocialButton extends React.Component<SocialButtonProps> {
    ref: HTMLAnchorElement;

    componentDidMount() {
        $(this.ref).tooltip();
    }

    render() {
        return (
            <a className={`btn btn-sm btn-social btn-social-icon btn-${this.props.provider.toLowerCase()}`}
               ref={(me: HTMLAnchorElement) => this.ref = me}
               onClick={() => this.props.onClick(this.props.provider)}
               data-toggle="tooltip"
               data-placement="top"
               title={`Sign in with ${this.props.provider}`}>
                <span className={`fa fa-${this.props.provider.toLowerCase()}`}/>
            </a>)
    }
}
