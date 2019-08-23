import {Kotoed} from "../../util/kotoed-api";
import * as React from "react";
import UrlPattern = Kotoed.UrlPattern;

type LinkProps = {
    pattern: string,
    [key: string]: string | number
}

export class KotoedLink extends React.PureComponent<LinkProps> {
    constructor(props: LinkProps) {
        super(props);
        this.state = {};
    }

    render(): JSX.Element {
        return <a href={UrlPattern.reverse(this.props.pattern, { ...this.props })}>{this.props.children}</a>
    }
}
