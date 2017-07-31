
import * as EventBus from "vertx3-eventbus-client";

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

    onerror: (error: Error) => boolean;  // Public field like in original EventBus

    constructor(url: string, options?: any) {
        this._isOpen = false;
        this.url = url;
        this.options = options;
        this.setUp();
    }

    private setUp = () => {
        this.eb = new EventBus(this.url, this.options);
        this.eb.onclose = () => {
            this._isOpen = false;
            if (!this.explicitlyClosed)
                setTimeout(this.setUp, RETRY_INTERVAL)  // TODO think about better reopen strategy
        };

        this.eb.onopen = () => {
            this._isOpen = true;
        };

        for (let handler of this.handlers) {
            this.eb.registerHandler(handler.address, handler.headers, handler.callback);
        }

    };

    get isOpen() {
        return this._isOpen;
    }

    registerHandler(address: string, headers: EventBusHeaders, callback: (error: Error, message: any) => void): void {
        this.handlers.push({
            address,
            headers,
            callback
        });
        this.eb.registerHandler(address, headers, callback);
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
                         headers?: EventBusHeaders): Promise<Reply> {
        return this.awaitOpen().then(() => new Promise<Reply>((resolve, reject) => {
            this.eb.send(address, message, headers, (error, message: EventBusReply<Reply>) => {
                if (error)
                    reject(error);
                else
                    resolve(message.body); // TODO maybe we need the whole reply?
            });
        }));
    }


    publish<Request>(address: string, message: Request, headers?: EventBusHeaders): Promise<void> {
        return this.awaitOpen().then(() => new Promise<void>((resolve, reject) => {
            this.eb.publish(address, message, headers);
            resolve();
        }));
    };

    close(): void {
        this.eb.close();
        this.explicitlyClosed = true;
        this._isOpen = false;
    };


}
