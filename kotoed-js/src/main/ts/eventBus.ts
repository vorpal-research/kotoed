
import {AsyncEventBus} from "./util/vertx";
import {fromLocationHost} from "./util/url";
import {keysToCamelCase, keysToSnakeCase} from "./util/stringCase";
import {Kotoed} from "./util/kotoed-api";

export const eventBus = new AsyncEventBus(
    fromLocationHost(Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.EventBus, {})),
    keysToSnakeCase,
    keysToCamelCase
);
