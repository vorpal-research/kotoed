import * as React from "react";
import {Kotoed} from "../../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;

export type LinkData = {
    entity: string
    id: string
}

export type NotificationData = {
    id: number,
    contents: string,
    linkTo?: LinkData
}

export interface NotificationDisplayProps {
    data: NotificationData
}

interface NotificationDisplayState {
    hidden: boolean
}

export class NotificationDisplay extends React.PureComponent<NotificationDisplayProps, NotificationDisplayState> {
    constructor(props: NotificationDisplayProps) {
        super(props);
        this.state = { hidden: false }
    }

    private onClick = () => { this.setState({ hidden: true }) };

    render() {
        return <a className="list-group-item"
                  href={ UrlPattern.reverse(UrlPattern.Notification.ById, { id : this.props.data.id }) }
                  target="_blank"
                  hidden={this.state.hidden}
                  onClick={this.onClick}
        >
            <span dangerouslySetInnerHTML={{ __html: this.props.data.contents }} />
        </a>
    }
}
