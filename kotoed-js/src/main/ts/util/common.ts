import * as _ from "lodash";

export function sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export function identity<T>(arg: T): T {
    return arg;
}

export function doNothing() : void {}

export function run<T>(body: () => T): T { return body() }

export function typedKeys<T>(obj: T) {
    return Object.keys(obj) as Array<keyof T>
}

export function pick<T extends {}, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
    return _.pick(obj, keys)
}
