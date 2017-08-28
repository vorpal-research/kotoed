import pathToRegexp = require("path-to-regexp");

import {Generated} from "./kotoed-generated";

export namespace Kotoed {
    export const Address = Generated.Address;

    export const UrlPattern = {
        ...Generated.UrlPattern,

        reverse(pattern: string, params: { [name: string]: string | number }, star: string | number = ""): string {
            let url = pattern;
            for (let k in params) {
                url = url.replace(`:${k}`, `${params[k]}`)
            }

            url = url.replace("*", `${star}`);

            return url
        },

        tryResolve(pattern: string, url: string): Map<string | number, string> | undefined {
            let keys: Array<pathToRegexp.Key> = [];
            let re = pathToRegexp(pattern, keys);
            let match = re.exec(url);

            if (match === null)
                return undefined;

            let res = new Map<string | number, string>();

            for (let i = 1; i < match.length; i++) {
                let keyIx = i - 1;
                res.set(keys[keyIx].name, match[i])
            }

            return res;
        }
    };

}
