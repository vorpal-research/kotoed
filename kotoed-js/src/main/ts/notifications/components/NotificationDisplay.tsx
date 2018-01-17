import * as React from "react";
import {Kotoed} from "../../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {sendAsync} from "../../views/components/common";
import {MouseEvent} from "react";

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

    makeProperLink = (link: LinkData) => {
        return UrlPattern.reverse(UrlPattern.Redirect.ById, link)
    };

    onClick = async (e: MouseEvent<HTMLAnchorElement>) => {
        if (!this.props.data.linkTo)
            return;
        e.preventDefault();
        // We have to await, otherwise it races with new page loading
        await sendAsync(Kotoed.Address.Api.Notification.MarkRead, { id: this.props.data.id });
        window.location.href = this.makeProperLink(this.props.data.linkTo)
    };

    render() {
        return <a className="list-group-item"
                  href="#"
                  onClick={this.onClick}
        >
            <span dangerouslySetInnerHTML={{ __html: this.props.data.contents }} />
        </a>
    }
}
