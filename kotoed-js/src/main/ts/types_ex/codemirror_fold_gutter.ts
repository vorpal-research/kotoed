import * as cm from "codemirror"

// TODO This is totally fucked up way of extending existing .d.ts definitions.
// TODO Rewrite it properly.

declare module "codemirror" {
    export interface EditorConfiguration {
        foldGutter: boolean
    }
}
