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

export class NotificationDisplay extends React.PureComponent<NotificationDisplayProps> {
    constructor(props: NotificationDisplayProps) {
        super(props)
    }

    render() {
        return <a className="list-group-item"
                  href={ UrlPattern.reverse(UrlPattern.Notification.ById, { id : this.props.data.id }) }
                  target="_blank"
        >
            <span dangerouslySetInnerHTML={{ __html: this.props.data.contents }} />
        </a>
    }
}
