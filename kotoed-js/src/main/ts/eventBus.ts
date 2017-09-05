
import {AsyncEventBus, isReplyError, ReplyError} from "./util/vertx";
import {fromLocationHost} from "./util/url";
import {keysToCamelCase, keysToSnakeCase} from "./util/stringCase";
import {Kotoed} from "./util/kotoed-api";
import snafuDialog from "./util/snafuDialog"


export class SoftError {
    message: string;

    constructor(message: string) {
        this.message = message
    }
}

export function isSnafu(e: ReplyError | Error) {
    return !isReplyError(e) || (isReplyError(e) && e.failureCode !== 409); // just because
}

function makeMsg(e: ReplyError | Error) {
    if (isReplyError(e) && e.failureCode === 409) {
        return "Unique constraint violation"
    } else if (e.message) {
        return e.message
    } else {
        return "Unknown remoteError"
    }
}

export function defaultErrorHandler(e: Error | ReplyError) {
    if (isSnafu(e)) {
        snafuDialog();
        throw e;
    } else {
        throw new SoftError(makeMsg(e));
    }
}

export function fallThroughErrorHandler(e: Error | ReplyError) {
    throw e;
}

export const eventBus = new AsyncEventBus(
    fromLocationHost(Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.EventBus, {})),
    keysToSnakeCase,
    keysToCamelCase,
    undefined,
    defaultErrorHandler
);
