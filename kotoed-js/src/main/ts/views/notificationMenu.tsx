import * as React from "react";
import {render} from "react-dom";
import {sendAsync} from "./components/common";
import {Kotoed} from "../util/kotoed-api";

interface LinkData {
    entity: string
    id: string
}

interface NotificationDisplayProps {
    data: {
        contents: string,
        linkTo: LinkData
    }
}

class NotificationDisplay extends React.PureComponent<NotificationDisplayProps> {
    constructor(props: NotificationDisplayProps) {
        super(props)
    }

    makeProperLink = (link: LinkData) => {
        return `/views/${link.entity}/id/${link.id}`
    };

    render() {
        return <a className="list-group-item" href={this.makeProperLink(this.props.data.linkTo)}>
            <span dangerouslySetInnerHTML={{ __html: this.props.data.contents }} />
        </a>
    }
}

interface NotificationMenuProps {}

interface NotificationMenuState {
    currentNotifications: Array<any>
}

class NotificationMenu extends React.Component<NotificationMenuProps, NotificationMenuState> {
    constructor(props: NotificationMenuProps) {
        super(props);
        this.state = {
            currentNotifications: []
        }
    }

    componentDidMount() {
        sendAsync(Kotoed.Address.Api.Notification.RenderCurrent, {})
            .then((data: Array<any>) => this.setState({ currentNotifications: data }))
    }

    render() {
        return <div className="list-group">
            {
                this.state.currentNotifications.map( (obj: any, index) =>
                    <NotificationDisplay data={obj} key={`notification-item-${index}`} />
                )
            }
        </div>
    }
}

render(
    <li><NotificationMenu /></li>,
    document.getElementById('notifications-menu')
);
