
import {AsyncEventBus} from "./util/vertx";
import {fromLocationHost} from "./util/url";
import {keysToCamelCase, keysToSnakeCase} from "./util/stringCase";

export const eventBus = new AsyncEventBus(
    fromLocationHost("eventbus"),
    keysToSnakeCase,
    keysToCamelCase
);
