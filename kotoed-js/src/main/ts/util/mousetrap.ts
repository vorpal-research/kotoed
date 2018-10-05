import * as Mousetrap from "mousetrap"
import "mousetrap/plugins/global-bind/mousetrap-global-bind.js"

export interface MousetrapInstance {
    stopCallback: (e: ExtendedKeyboardEvent, element: Element, combo: string) => boolean;
    bind(keys: string|string[], callback: (e: ExtendedKeyboardEvent, combo: string) => any, action?: string): void;
    unbind(keys: string|string[], action?: string): void;
    trigger(keys: string, action?: string): void;
    reset(): void;
}

export default Mousetrap

export function withPreventDefault(callback: (e: Event) => void) {
    return function (e: Event) {
        if (e.preventDefault) {
            e.preventDefault();
        } else {
            // internet explorer
            e.returnValue = false;
        }
        callback(e);
    }

}