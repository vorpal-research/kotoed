import * as React from "react";
import {Kotoed} from "../../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {sendAsync} from "../../views/components/common";

export type LinkData = {
    entity: string
    id: string
}

export type NotificationData = {
    id: number,
    contents: string,
    linkTo: LinkData
}

export interface NotificationDisplayProps {
    data: NotificationData
}

export class NotificationDisplay extends React.PureComponent<NotificationDisplayProps> {
    constructor(props: NotificationDisplayProps) {
        super(props)
    }

    makeProperLink = (link: LinkData) => {
        return UrlPattern.reverse(UrlPattern.Redirect.ById, link)
    };

    onClick = () => {
        sendAsync(Kotoed.Address.Api.Notification.MarkRead, { id: this.props.data.id }).then()
    };

    render() {
        return <a className="list-group-item"
                  href={this.makeProperLink(this.props.data.linkTo)}
                  onClick={this.onClick}
        >
            <span dangerouslySetInnerHTML={{ __html: this.props.data.contents }} />
        </a>
    }
}
