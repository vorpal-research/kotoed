import * as React from "react";
import {render} from "react-dom";
import {sendAsync} from "./components/common";
import {Kotoed} from "../util/kotoed-api";

interface NotificationDisplayProps {
    type: string,
    body: object
}

class NotificationDisplay extends React.PureComponent<NotificationDisplayProps> {
    constructor(props: NotificationDisplayProps) {
        super(props)
    }

    render() {
        return <a className="list-group-item">
            {this.props.body.toString()}
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
        sendAsync(Kotoed.Address.Api.Notification.Current, {})
            .then((data: Array<any>) => this.setState({ currentNotifications: data }))
    }

    render() {
        return <div className="list-group">
            {
                this.state.currentNotifications.map( (obj: any, index) =>
                    <NotificationDisplay type={obj.type} body={obj.body} key={"notification-item-" + index} />
                )
            }
        </div>
    }
}

render(
    <li><NotificationMenu /></li>,
    document.getElementById('notifications-menu')
);
