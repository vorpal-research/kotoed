import * as React from "react"

import "less/bootstrap-social.less"

import "font-awesome/less/font-awesome.less"


interface SocialButtonProps {
    provider: string
    onClick: (provider: string) => void
}

export default class SocialButton extends React.Component<SocialButtonProps> {
    render() {
        return (
            <button className={`btn btn-lg btn-social btn-block btn-${this.props.provider.toLowerCase()}`}
                    onClick={() => this.props.onClick(this.props.provider)}>
                <span className={`fa fa-${this.props.provider.toLowerCase()}`}/>
                Sign in with {this.props.provider}
            </button>)
    }
}
