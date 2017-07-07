/**
 * Created by gagarski on 7/5/17.
 */

// TODO remove me

import * as AceAjax from 'brace';

declare module 'brace' {
    export function define(name: string,
                           deps: string[],
                           fun: (acequire: (modulename: string) => any,
                                 exports: any,
                                 module: any) => any): void;  // TODO provide type
}