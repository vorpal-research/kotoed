import "res/kotoed3.png"
import "res/kotoed3w.png"

import {Kotoed} from "./util/kotoed-api";
export function imagePath(path: string) {
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Static, {}, `img/${path}`);
}

export function embeddedImage(base64: string | undefined): string | undefined {
    if(!base64) return undefined;
    return `data:image/png;base64,${base64}`
}
