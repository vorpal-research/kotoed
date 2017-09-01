import * as React from "react";
import * as NotificationSystem from "react-notification-system"
import {render} from "react-dom";
import {EventbusMessage} from "../util/vertx";
import {NotificationData, NotificationDisplay} from "./components/NotificationDisplay";
import {doNothing, run} from "../util/common";
import {eventBus} from "../eventBus";
import {myDatabaseId} from "../login/remote";
import {Kotoed} from "../util/kotoed-api";

class KotoedNotificationSystem extends React.PureComponent {
    constructor(props: {}) {
        super(props);
    }

    _notificationSystem: NotificationSystem.System | null = null;

    onNewNotification = async (message: EventbusMessage<NotificationData>) => {
        this._notificationSystem!.addNotification({
            level: "info",
            position: "tr",
            autoDismiss: 10,
            uid: message.body.id,
            children: <div>
                <div className="vspace-10" />
                <NotificationDisplay data={message.body} />
            </div>
        });
    };


    componentDidMount() {
        run(async () => {
            let eb = eventBus.awaitOpen();
            let me = await myDatabaseId();
            await eb;
            eventBus.registerHandler(
                Kotoed.Address.Api.Notification.pushRendered(me),
                {},
                async (_, message) => this.onNewNotification(message)
            );
        }).catch(doNothing).then(doNothing);
    }

    render() {
        return <NotificationSystem ref={(ns: NotificationSystem.System) => { this._notificationSystem = ns }} />
    }
}

render(
    <KotoedNotificationSystem />,
    document.getElementById('kotoed-notification-container')
);

