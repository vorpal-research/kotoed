export function sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export function identity<T>(arg: T): T {
    return arg;
}

export function doNothing() : void {}

export function typedKeys<T>(obj: T) {
    return Object.keys(obj) as Array<keyof T>
}
