
import {AsyncEventBus} from "./util/vertx";
import {fromLocationHost} from "./util/url";

export const eventBus = new AsyncEventBus(fromLocationHost("eventbus"));
