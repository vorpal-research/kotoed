import "res/kotoed3.png"
import "res/kotoed3w.png"

import {Kotoed} from "./util/kotoed-api";
export function imagePath(path: string) {
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Static, {}, `img/${path}`);
}
