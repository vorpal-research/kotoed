import * as _ from "lodash"

function convertStringKeys(obj: any, f: (key: string) => string) {
    // TODO maybe we should allow converting some objects that have some other constructor
    if (typeof obj !== "object" || obj === null || (obj.constructor !== Object && obj.constructor !== Array))
        return obj;

    let newObj: any = Array.isArray(obj) ? [] : {};

    for (let key in obj) {
        if (!obj.hasOwnProperty(key))
            continue;

        let newKey;

        if (typeof key === "string") {
            newKey = f(key);
            if (key.startsWith("@")) newKey = `@${newKey}`
        } else {
            newKey = key;
        }

        newObj[newKey] = convertStringKeys(obj[key], f);
    }

    return newObj;
}

export function keysToCamelCase(obj: any): any {
    return convertStringKeys(obj, _.camelCase);
}

export function keysToSnakeCase(obj: any): any {
    return convertStringKeys(obj, _.snakeCase);
}
