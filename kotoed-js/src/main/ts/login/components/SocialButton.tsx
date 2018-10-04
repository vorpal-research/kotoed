import * as React from "react"

import "less/bootstrap-social.less"
import "less/socialButtonsEx.less"

import "@fortawesome/fontawesome-free/less/fontawesome.less"
import "@fortawesome/fontawesome-free/less/solid.less"
import "@fortawesome/fontawesome-free/less/brands.less"

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
                <span className={`fab fa-${this.props.provider.toLowerCase()}`}/>
            </a>)
    }
}
