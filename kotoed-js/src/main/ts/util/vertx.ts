import * as EventBus from "vertx3-eventbus-client";
import {identity} from "./common";

class EventBusHeaders {
    [name: string]: string
}

const RETRY_INTERVAL = 1000;

interface EventBusHandler {
    address: string;
    headers: EventBusHeaders;
    callback: (error: Error, message: any) => any;
}

export class EventBusError extends Error {
    constructor(cause: string) {
        super(cause)
    }
}

export interface EventBusReply<T> {
    address: string
    body: T
    type: string
}

const RECONNECT_RETRIES = 5;

export interface ReplyError {
    failureCode: number
    failureType: "RECIPIENT_FAILURE" | "TIMEOUT" | "NO_HANDLERS"
    message: string
}

export function isReplyError(e: Error | ReplyError): e is ReplyError {
    return e.hasOwnProperty("failureCode") && e.hasOwnProperty("failureType");
}

export interface EventbusMessage<Body = any> {
    body: Body,
    address: string,
    type: string
}

/**
 * This event bus allows awaitOpen, sendAsync and reopens connection after loosing it
 */
export class AsyncEventBus {
    private url: string;
    private options: any;
    private eb: EventBus.EventBus;
    private _isOpen: boolean = false;
    private explicitlyClosed: boolean = false;
    private handlers: Array<EventBusHandler> = [];
    private onError?: (error: Error | ReplyError) => void;
    private nClosed: number = 0;

    // Heil POSIX!
    private htonJ: (h: any) => any = obj => obj;
    private ntohJ: (n: any) => any = obj => obj;

    constructor(url: string,
                htonJ: (host: any) => any = identity,
                ntohJ: (network: any) => any = identity,
                options?: any,
                onError?: (error: Error) => void) {
        this._isOpen = false;
        this.url = url;
        this.options = options;
        this.onError = onError;
        this.htonJ = htonJ;
        this.ntohJ = ntohJ;
        this.setUp();
    }

    private setUp = () => {
        this.eb = new EventBus(this.url, this.options);
        let oldOnError = this.eb.onerror; // We'll need it
        this.eb.onerror = (error) => {
            if (this.onError) {
                this.onError(error);
            }
            oldOnError(error);
        };

        this.eb.onopen = () => {
            this._isOpen = true;
            this.nClosed = 0;
        };

        this.eb.onclose = () => {
            this._isOpen = false;

            if (this.nClosed === RECONNECT_RETRIES) {
                this.onError && this.onError(new Error("Max retries exceeded"));
                return;
            }
            this.nClosed++;
            if (!this.explicitlyClosed)
                setTimeout(this.setUp, RETRY_INTERVAL)  // TODO think about better reopen strategy
        };


        for (let handler of this.handlers) {
            this.eb.registerHandler(handler.address, handler.headers, handler.callback);
        }

    };

    registerHandler(address: string, headers: EventBusHeaders, callback: (error: Error, message: EventbusMessage) => void): void {
        this.handlers.push({
            address,
            headers,
            callback
        });
        this.eb.registerHandler(address, headers, (error, message) => callback(error, this.ntohJ(message)));
    };

    awaitOpen(): Promise<void> {
        if (this.explicitlyClosed)
            throw new EventBusError("EventBus is explicitly closed");
        else if (this._isOpen)
            return Promise.resolve();
        else {
            let oldOnOpen = this.eb.onopen;
            return new Promise((resolve) => {
                this.eb.onopen = () => {
                    oldOnOpen && oldOnOpen();
                    resolve()
                }
            });
        }
    }

    send<Request, Reply>(address: string,
                         message: Request,
                         headers?: EventBusHeaders,
                         onError = this.onError): Promise<Reply> {
        return this.awaitOpen().then(() => new Promise<Reply>((resolve, reject) => {
            this.eb.send(address, this.htonJ(message), headers, (error, message: EventBusReply<Reply>) => {
                if (error) {
                    try {
                        onError && onError(error);
                    } catch (processedError) {
                        reject(processedError);
                    }
                } else
                    resolve(this.ntohJ(message.body)); // TODO maybe we need the whole reply?
            });
        }));
    }


    publish<Request>(address: string, message: Request, headers?: EventBusHeaders): Promise<void> {
        return this.awaitOpen().then(() => new Promise<void>((resolve, reject) => {
            this.eb.publish(address, this.htonJ(message), headers);
            resolve();
        }));
    };

    close(): void {
        this.eb.close();
        this.explicitlyClosed = true;
        this._isOpen = false;
    };


}
