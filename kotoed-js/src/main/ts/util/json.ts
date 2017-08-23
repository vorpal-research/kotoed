import {isString} from "util";

export function angleBracketReplacer(key: string, value: any): any {
    return isString(value)
        ? value.replace("<", "&lt;").replace(">", "&gt;")
        : value;
}
