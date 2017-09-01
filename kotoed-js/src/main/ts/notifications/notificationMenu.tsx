import * as React from "react";
import * as NotificationSystem from "react-notification-system"
import {render} from "react-dom";
import {sendAsync, setStateAsync} from "../views/components/common";
import {Kotoed} from "../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {eventBus} from "../eventBus";
import {myDatabaseId} from "../login/remote";
import {doNothing, run} from "../util/common";
import {EventbusMessage} from "../util/vertx";
import {NotificationDisplay} from "./components/NotificationDisplay";

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

    invalidate = async () => {
        let data = await sendAsync<{}, any[]>(Kotoed.Address.Api.Notification.RenderCurrent, {});
        await setStateAsync(this,{ currentNotifications: data });
    };

    componentDidMount() {
        run(async () => {
            this.invalidate();
            let eb = eventBus.awaitOpen();
            let me = await myDatabaseId();
            await eb;
            eventBus.registerHandler(
                Kotoed.Address.Api.Notification.pushRendered(me),
                {},
                async (_, message) => this.invalidate()
            );
        }).catch(doNothing).then(doNothing);
    }

    render() {
        if(this.state.currentNotifications.length != 0) {
            return <div className="list-group">
                {
                    this.state.currentNotifications.map( (obj: any, index) =>
                        <NotificationDisplay data={obj} key={`notification-item-${index}`} />
                    )
                }
            </div>
        } else {
            return <div>No current notifications found</div>
        }
    }
}

render(
    <li><NotificationMenu /></li>,
    document.getElementById('notifications-menu')
);
